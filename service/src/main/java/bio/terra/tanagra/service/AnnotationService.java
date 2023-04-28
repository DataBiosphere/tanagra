package bio.terra.tanagra.service;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.app.configuration.TanagraExportConfiguration;
import bio.terra.tanagra.db.AnnotationDao;
import bio.terra.tanagra.db.AnnotationValueDao;
import bio.terra.tanagra.db.CohortDao1;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.artifact.AnnotationV1;
import bio.terra.tanagra.service.artifact.AnnotationValueV1;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.utils.GcsUtils;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.Underlay.MappingType;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
  private final AnnotationValueDao annotationValueDao;
  private final CohortDao1 cohortDao;
  private final CohortService cohortService;
  private final FeatureConfiguration featureConfiguration;
  private final TanagraExportConfiguration tanagraExportConfiguration;
  private final UnderlaysService underlaysService;

  @Autowired
  public AnnotationService(
      AnnotationDao annotationDao,
      AnnotationValueDao annotationValueDao,
      CohortDao1 cohortDao,
      CohortService cohortService,
      FeatureConfiguration featureConfiguration,
      TanagraExportConfiguration tanagraExportConfiguration,
      UnderlaysService underlaysService) {
    this.annotationDao = annotationDao;
    this.annotationValueDao = annotationValueDao;
    this.cohortDao = cohortDao;
    this.cohortService = cohortService;
    this.featureConfiguration = featureConfiguration;
    this.tanagraExportConfiguration = tanagraExportConfiguration;
    this.underlaysService = underlaysService;
  }

  /** Create a new annotation. */
  public void createAnnotation(
      String studyId, String cohortRevisionGroupId, AnnotationV1 annotation) {
    featureConfiguration.artifactStorageEnabledCheck();
    annotationDao.createAnnotation(studyId, cohortRevisionGroupId, annotation);
  }

  /** Delete an existing annotation. */
  public void deleteAnnotation(String studyId, String cohortRevisionGroupId, String annotationId) {
    featureConfiguration.artifactStorageEnabledCheck();
    annotationDao.deleteAnnotation(studyId, cohortRevisionGroupId, annotationId);
  }

  /** Retrieves a list of all annotations for a cohort. */
  public List<AnnotationV1> getAllAnnotations(
      String studyId, String cohortRevisionGroupId, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return annotationDao.getAllAnnotations(studyId, cohortRevisionGroupId, offset, limit);
  }

  /** Retrieves a list of annotations by ID. */
  public List<AnnotationV1> getAnnotations(
      String studyId,
      String cohortRevisionGroupId,
      List<String> annotationIds,
      int offset,
      int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return annotationDao.getAnnotationsMatchingList(
        studyId, cohortRevisionGroupId, new HashSet<>(annotationIds), offset, limit);
  }

  /** Retrieves an annotation by ID. */
  public AnnotationV1 getAnnotation(
      String studyId, String cohortRevisionGroupId, String annotationId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return annotationDao.getAnnotation(studyId, cohortRevisionGroupId, annotationId);
  }

  /**
   * Update an existing annotation. Currently, can change the annotation's display name or
   * description.
   */
  public AnnotationV1 updateAnnotation(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      @Nullable String displayName,
      @Nullable String description) {
    featureConfiguration.artifactStorageEnabledCheck();
    annotationDao.updateAnnotation(
        studyId, cohortRevisionGroupId, annotationId, displayName, description);
    return annotationDao.getAnnotation(studyId, cohortRevisionGroupId, annotationId);
  }

  /** Create a new annotation value. */
  public AnnotationValueV1 createAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      AnnotationValueV1 annotationValue) {
    featureConfiguration.artifactStorageEnabledCheck();
    validateAnnotationValueDataType(
        studyId, cohortRevisionGroupId, annotationId, annotationValue.getLiteral());
    annotationValueDao.createAnnotationValue(
        studyId, cohortRevisionGroupId, annotationId, reviewId, annotationValue);
    return annotationValueDao.getAnnotationValue(
        studyId,
        cohortRevisionGroupId,
        annotationId,
        reviewId,
        annotationValue.getAnnotationValueId());
  }

  /** Delete an existing annotation value. */
  public void deleteAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      String annotationValueId) {
    featureConfiguration.artifactStorageEnabledCheck();
    annotationValueDao.deleteAnnotationValue(
        studyId, cohortRevisionGroupId, annotationId, reviewId, annotationValueId);
  }

  /**
   * Update an existing annotation value. Currently, can change the annotation value's literal only.
   */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public AnnotationValueV1 updateAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      String annotationValueId,
      Literal literal) {
    featureConfiguration.artifactStorageEnabledCheck();
    validateAnnotationValueDataType(studyId, cohortRevisionGroupId, annotationId, literal);
    annotationValueDao.updateAnnotationValue(
        studyId, cohortRevisionGroupId, annotationId, reviewId, annotationValueId, literal);
    return annotationValueDao.getAnnotationValue(
        studyId, cohortRevisionGroupId, annotationId, reviewId, annotationValueId);
  }

  /** Create or update an annotation value. Only the annotation value's literal can be updated. */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public AnnotationValueV1 createUpdateAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      AnnotationValueV1 annotationValue) {
    featureConfiguration.artifactStorageEnabledCheck();
    validateAnnotationValueDataType(
        studyId, cohortRevisionGroupId, annotationId, annotationValue.getLiteral());
    annotationValueDao.createUpdateAnnotationValue(
        studyId, cohortRevisionGroupId, annotationId, reviewId, annotationValue);
    return annotationValueDao.getAnnotationValue(
        studyId,
        cohortRevisionGroupId,
        annotationId,
        reviewId,
        annotationValue.getAnnotationValueId());
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
        cohortDao.getCohortLatestVersion(studyId, cohortRevisionGroupId).getCohortId();
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
          AnnotationV1 annotation =
              getAnnotation(studyId, cohortRevisionGroupId, value.getAnnotationId());
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
    List<AnnotationV1> annotations =
        getAllAnnotations(studyId, cohortId, /*offset=*/ 0, /*limit=*/ Integer.MAX_VALUE);
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

  /**
   * Throw if the annotation value data type does not match the annotation data type or enum values.
   */
  private void validateAnnotationValueDataType(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      Literal annotationValueLiteral) {
    AnnotationV1 annotation =
        annotationDao.getAnnotation(studyId, cohortRevisionGroupId, annotationId);
    if (!annotation.getDataType().equals(annotationValueLiteral.getDataType())) {
      throw new BadRequestException(
          String.format(
              "Annotation value data type (%s) does not match the annotation data type (%s)",
              annotationValueLiteral.getDataType(), annotation.getDataType()));
    }

    switch (annotationValueLiteral.getDataType()) {
      case STRING:
        if (annotationValueLiteral.getStringVal() == null) {
          throw new BadRequestException("String value cannot be null");
        }
        break;
      case INT64:
        if (annotationValueLiteral.getInt64Val() == null) {
          throw new BadRequestException("Integer value cannot be null");
        }
        break;
      case BOOLEAN:
        if (annotationValueLiteral.getBooleanVal() == null) {
          throw new BadRequestException("Boolean value cannot be null");
        }
        break;
      case DATE:
        if (annotationValueLiteral.getDateVal() == null) {
          throw new BadRequestException("Date value cannot be null");
        }
        break;
      case DOUBLE:
        if (annotationValueLiteral.getDoubleVal() == null) {
          throw new BadRequestException("Double value cannot be null");
        }
        break;
      default:
        throw new SystemException("Unknown data type: " + annotationValueLiteral.getDataType());
    }

    if (!annotation.getEnumVals().isEmpty()
        && !annotation.getEnumVals().contains(annotationValueLiteral.getStringVal())) {
      throw new BadRequestException(
          String.format(
              "Annotation value (%s) is not one of the annotation enum values (%s)",
              annotationValueLiteral.getStringVal(), String.join(",", annotation.getEnumVals())));
    }
  }
}
