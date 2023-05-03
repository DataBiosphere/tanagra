package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.app.configuration.TanagraExportConfiguration;
import bio.terra.tanagra.db.AnnotationDao;
import bio.terra.tanagra.db.AnnotationValueDao;
import bio.terra.tanagra.db.CohortDao1;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.AnnotationValueV1;
import bio.terra.tanagra.service.model.AnnotationKey;
import bio.terra.tanagra.service.model.AnnotationValue;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.Review;
import bio.terra.tanagra.service.utils.GcsUtils;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.Underlay.MappingType;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.time.OffsetDateTime;
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

  private final AnnotationValueDao annotationValueDao;
  private final CohortDao1 cohortDao1;

  @Autowired
  public AnnotationService(
      AnnotationDao annotationDao,
      AnnotationValueDao annotationValueDao,
      CohortDao1 cohortDao1,
      CohortService cohortService,
      ReviewService reviewService,
      FeatureConfiguration featureConfiguration,
      TanagraExportConfiguration tanagraExportConfiguration,
      UnderlaysService underlaysService) {
    this.annotationDao = annotationDao;
    this.annotationValueDao = annotationValueDao;
    this.cohortDao1 = cohortDao1;
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

    // Filter the values, keeping only the most recent ones and those that belong to the specified
    // revision.
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

  /** Retrieves a list of all annotation values for a review. */
  public List<AnnotationValueV1> getAnnotationValues(String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return annotationValueDao.getAnnotationValues(reviewId);
  }

  /**
   * Say for an annotation:
   *
   * <pre>
   *            Person 1     Person 2
   * Review 1   Value 1      Value 1
   * Review 2   Value 2
   * </pre>
   *
   * For each person, return the latest value. For Person 1, this is Value 2 from Review 2. For
   * Person 2, this is Value 1 from Review 1.
   *
   * <p>A (sparse) table is returned:
   *
   * <pre>
   *                   Annotation 1     Annotation 2
   *  Person id 1      Value 2          Value 3
   *  Person id 2      Value 1
   * </pre>
   *
   * @return for all annotations and all entity instances, the annotation value from the latest
   *     review
   */
  public Table<String, String, String> getAnnotationValuesForLatestReview(
      String studyId, String cohortRevisionGroupId) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Get values for all reviews for cohort
    String cohortId =
        cohortDao1.getCohortLatestVersion(studyId, cohortRevisionGroupId).getCohortId();
    List<Pair<OffsetDateTime, AnnotationValueV1>> reviewCreateDateAndValues =
        annotationValueDao.getAnnotationValuesForCohort(cohortId);

    // Sort values (for all reviews) by review creation date
    reviewCreateDateAndValues.sort(
        Comparator.comparingLong(
            reviewCreateDateAndValue -> reviewCreateDateAndValue.getLeft().toEpochSecond()));
    List<AnnotationValueV1> sortedValuesForAllReviews =
        reviewCreateDateAndValues.stream().map(Pair::getRight).collect(Collectors.toList());

    // Traverse through sorted values and update table-to-be-returned. By the time we're done, we
    // will have latest value for each table cell.
    Table<String, String, String> tableToReturn = HashBasedTable.create();
    sortedValuesForAllReviews.forEach(
        value -> {
          AnnotationKey annotation =
              getAnnotationKey(studyId, cohortRevisionGroupId, value.getAnnotationId());
          tableToReturn.put(
              value.getEntityInstanceId(), // row
              annotation.getDisplayName(), // column
              value.getLiteral().toString() // value
              );
        });
    return tableToReturn;
  }

  /**
   * @param values is a table where row is entity instance id, column is annotation name, value is
   *     annotation value
   * @return GCS signed URL of GCS file containing annotation values TSV. Columns are: entity
   *     instance id, all annotation names. Cells contain annotation values.
   */
  public String writeAnnotationValuesToGcs(
      String studyId, String cohortId, Table<String, String, String> values) {
    // Convert table of annotation values to String representing CSV file
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
    List<AnnotationKey> annotations =
        listAnnotationKeys(
            ResourceIdCollection.allResourceIds(),
            studyId,
            cohortId,
            /*offset=*/ 0,
            /*limit=*/ Integer.MAX_VALUE);
    annotations.forEach(
        annotation -> {
          columnHeaders.append(String.format("\t%s", annotation.getDisplayName()));
        });
    StringBuilder fileContents = new StringBuilder(columnHeaders + "\n");
    values
        .rowKeySet()
        .forEach(
            entityInstanceId -> {
              StringBuilder row = new StringBuilder(entityInstanceId);
              annotations.forEach(
                  annotation -> {
                    String value =
                        values.contains(entityInstanceId, annotation.getDisplayName())
                            ? values.get(entityInstanceId, annotation.getDisplayName())
                            : "";
                    row.append("\t" + value);
                  });
              fileContents.append(String.format(row + "\n"));
            });
    LOGGER.info(
        "Writing annotation values to CSV for study {} cohort {}:\n{}",
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
}
