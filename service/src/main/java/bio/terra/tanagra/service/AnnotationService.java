package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.app.configuration.TanagraExportConfiguration;
import bio.terra.tanagra.db.AnnotationDao;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.AnnotationKey;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Review;
import bio.terra.tanagra.service.utils.GcsUtils;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.Underlay.MappingType;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class AnnotationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationService.class);
  private final AnnotationDao annotationDao;
  private final CohortService cohortService;
  private final ReviewService reviewService;
  private final FeatureConfiguration featureConfiguration;
  private final TanagraExportConfiguration tanagraExportConfiguration;
  private final UnderlaysService underlaysService;

  @Autowired
  public AnnotationService(
      AnnotationDao annotationDao,
      CohortService cohortService,
      ReviewService reviewService,
      FeatureConfiguration featureConfiguration,
      TanagraExportConfiguration tanagraExportConfiguration,
      UnderlaysService underlaysService) {
    this.annotationDao = annotationDao;
    this.cohortService = cohortService;
    this.reviewService = reviewService;
    this.featureConfiguration = featureConfiguration;
    this.tanagraExportConfiguration = tanagraExportConfiguration;
    this.underlaysService = underlaysService;
  }

  public AnnotationKey createAnnotationKey(
      String studyId, String cohortId, AnnotationKey.Builder annotationKeyBuilder) {
    featureConfiguration.artifactStorageEnabledCheck();
    annotationDao.createAnnotationKey(cohortId, annotationKeyBuilder.build());
    return annotationDao.getAnnotationKey(cohortId, annotationKeyBuilder.getId());
  }

  public void deleteAnnotationKey(String studyId, String cohortId, String annotationKeyId) {
    featureConfiguration.artifactStorageEnabledCheck();
    annotationDao.deleteAnnotationKey(cohortId, annotationKeyId);
  }

  public List<AnnotationKey> listAnnotationKeys(
      ResourceIdCollection authorizedAnnotationKeyIds,
      String studyId,
      String cohortId,
      int offset,
      int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    if (authorizedAnnotationKeyIds.isAllResourceIds()) {
      return annotationDao.getAllAnnotationKeys(cohortId, offset, limit);
    } else {
      return annotationDao.getAnnotationKeysMatchingList(
          cohortId,
          authorizedAnnotationKeyIds.getResourceIds().stream()
              .map(ResourceId::getId)
              .collect(Collectors.toSet()),
          offset,
          limit);
    }
  }

  public AnnotationKey getAnnotationKey(String studyId, String cohortId, String annotationKeyId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return annotationDao.getAnnotationKey(cohortId, annotationKeyId);
  }

  public AnnotationKey updateAnnotationKey(
      String studyId,
      String cohortId,
      String annotationKeyId,
      @Nullable String displayName,
      @Nullable String description) {
    featureConfiguration.artifactStorageEnabledCheck();
    annotationDao.updateAnnotationKey(cohortId, annotationKeyId, displayName, description);
    return annotationDao.getAnnotationKey(cohortId, annotationKeyId);
  }

  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public void updateAnnotationValues(
      String studyId,
      String cohortId,
      String annotationKeyId,
      String reviewId,
      String instanceId,
      List<Literal> annotationValues) {
    featureConfiguration.artifactStorageEnabledCheck();
    AnnotationKey annotationKey = annotationDao.getAnnotationKey(cohortId, annotationKeyId);
    annotationValues.stream().forEach(av -> annotationKey.validateValue(av));
    annotationDao.updateAnnotationValues(
        cohortId, annotationKeyId, reviewId, instanceId, annotationValues);
  }

  public void deleteAnnotationValues(
      String studyId, String cohortId, String annotationKeyId, String reviewId, String instanceId) {
    updateAnnotationValues(
        studyId, cohortId, annotationKeyId, reviewId, instanceId, Collections.emptyList());
  }

  public List<AnnotationValue> listAnnotationValues(
      String studyId, String cohortId, @Nullable String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();

    int selectedVersion;
    if (reviewId != null) {
      // Look up the cohort revision associated with the specified review.
      Review review = reviewService.getReview(studyId, cohortId, reviewId);
      selectedVersion = review.getRevision().getVersion();
    } else {
      // No review is specified, so use the most recent cohort revision.
      Cohort cohort = cohortService.getCohort(studyId, cohortId);
      selectedVersion = cohort.getMostRecentRevision().getVersion();
    }
    LOGGER.debug("selectedVersion: {}", selectedVersion);

    // Fetch all the annotation values for this cohort.
    List<AnnotationValue.Builder> allValues = annotationDao.getAllAnnotationValues(cohortId);
    LOGGER.debug("allValues.size = {}", allValues.size());

    // Build a map of the values by key and instance id: annotation key id -> list of annotation
    // values
    Map<Pair<String, String>, List<AnnotationValue.Builder>> allValuesMap = new HashMap<>();
    allValues.stream()
        .forEach(
            v -> {
              Pair<String, String> keyAndInstance =
                  Pair.of(v.getAnnotationKeyId(), v.getInstanceId());
              List<AnnotationValue.Builder> valuesForKeyAndInstance =
                  allValuesMap.get(keyAndInstance);
              if (valuesForKeyAndInstance == null) {
                valuesForKeyAndInstance = new ArrayList<>();
                allValuesMap.put(keyAndInstance, valuesForKeyAndInstance);
              }
              valuesForKeyAndInstance.add(v);
            });

    // Filter the values, keeping only the most recent ones for each key-instance pair, and those
    // that belong to the specified revision.
    List<AnnotationValue> filteredValues = new ArrayList<>();
    allValuesMap.entrySet().stream()
        .forEach(
            keyValues -> {
              Pair<String, String> keyAndInstance = keyValues.getKey();
              List<AnnotationValue.Builder> allValuesForKeyAndInstance = keyValues.getValue();
              int maxVersionForKeyAndInstance =
                  allValuesForKeyAndInstance.stream()
                      .max(
                          Comparator.comparingInt(
                              AnnotationValue.Builder::getCohortRevisionVersion))
                      .get()
                      .getCohortRevisionVersion();

              List<AnnotationValue> filteredValuesForKey = new ArrayList<>();
              allValuesForKeyAndInstance.stream()
                  .forEach(
                      v -> {
                        boolean isMostRecent =
                            v.getCohortRevisionVersion() == maxVersionForKeyAndInstance;
                        boolean isPartOfSelectedReview =
                            (v.getCohortRevisionVersion() == selectedVersion);
                        if (isMostRecent || isPartOfSelectedReview) {
                          filteredValuesForKey.add(
                              v.isMostRecent(isMostRecent)
                                  .isPartOfSelectedReview(isPartOfSelectedReview)
                                  .build());
                        } else {
                          LOGGER.debug(
                              "filtering out av {} - {} - {} - {} ({}, {})",
                              v.build().getCohortRevisionVersion(),
                              v.build().getInstanceId(),
                              v.build().getAnnotationKeyId(),
                              v.build().getLiteral().getStringVal(),
                              maxVersionForKeyAndInstance,
                              selectedVersion);
                        }
                      });
              filteredValues.addAll(filteredValuesForKey);
            });
    return filteredValues;
  }

  public List<AnnotationValue> listAnnotationValues(String studyId, String cohortId) {
    return listAnnotationValues(studyId, cohortId, null);
  }

  public String exportAnnotationValuesToGcs(String studyId, String cohortId) {
    String fileContents = buildTsvStringForAnnotationValues(studyId, cohortId);
    LOGGER.info(
        "Writing annotation values to TSV for study {} cohort {}:\n{}",
        studyId,
        cohortId,
        fileContents);

    String projectId = tanagraExportConfiguration.getGcsBucketProjectId();
    String bucketName = tanagraExportConfiguration.getGcsBucketName();
    String fileName = "tanagra_export_annotations_" + System.currentTimeMillis() + ".tsv";
    if (StringUtils.isEmpty(projectId) || StringUtils.isEmpty(bucketName)) {
      throw new SystemException(
          "For export, gcsBucketProjectId and gcsBucketName properties must be set");
    }

    GcsUtils.writeGcsFile(projectId, bucketName, fileName, fileContents.toString());
    return bio.terra.tanagra.utils.GcsUtils.createSignedUrl(projectId, bucketName, fileName);
  }

  @VisibleForTesting
  public String buildTsvStringForAnnotationValues(String studyId, String cohortId) {
    // Build the column headers: id column name in source data, then annotation key display names.
    // Sort the annotation keys by display name, so that we get a consistent ordering.
    // e.g. person_id, key1, key2
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Underlay underlay = underlaysService.getUnderlay(cohort.getUnderlay());
    String primaryIdSourceColumnName =
        underlay
            .getPrimaryEntity()
            .getIdAttribute()
            .getMapping(MappingType.SOURCE)
            .getValue()
            .getColumnName();
    StringBuilder columnHeaders = new StringBuilder(primaryIdSourceColumnName);
    List<AnnotationKey> annotationKeys =
        listAnnotationKeys(
                ResourceIdCollection.allResourceIds(),
                studyId,
                cohortId,
                /*offset=*/ 0,
                /*limit=*/ Integer.MAX_VALUE)
            .stream()
            .sorted(Comparator.comparing(AnnotationKey::getDisplayName))
            .collect(Collectors.toList());
    annotationKeys.forEach(
        annotation -> {
          columnHeaders.append(String.format("\t%s", annotation.getDisplayName()));
        });
    StringBuilder fileContents = new StringBuilder(columnHeaders + "\n");

    // Get all the annotation values for the latest revision.
    List<AnnotationValue> annotationValues = listAnnotationValues(studyId, cohortId);

    // Convert the list of annotation values to a TSV-ready table.
    Table<String, String, String> tsvValues = HashBasedTable.create();
    annotationValues.forEach(
        value -> {
          AnnotationKey key = getAnnotationKey(studyId, cohortId, value.getAnnotationKeyId());
          tsvValues.put(
              value.getInstanceId(), // row
              key.getDisplayName(), // column
              value.getLiteral().toString() // value
              );
        });

    // Convert table of annotation values to String representing TSV file.
    // Sort the instance ids, so that we get a consistent ordering.
    tsvValues.rowKeySet().stream()
        .sorted()
        .forEach(
            instanceId -> {
              StringBuilder row = new StringBuilder(instanceId);
              annotationKeys.forEach(
                  annotationKey -> {
                    String tsvValue =
                        tsvValues.contains(instanceId, annotationKey.getDisplayName())
                            ? tsvValues.get(instanceId, annotationKey.getDisplayName())
                            : "";
                    row.append("\t" + tsvValue);
                  });
              fileContents.append(String.format(row + "\n"));
            });
    return fileContents.toString();
  }
}
