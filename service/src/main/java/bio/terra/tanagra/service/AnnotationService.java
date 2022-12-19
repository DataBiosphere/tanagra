package bio.terra.tanagra.service;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.AnnotationDao;
import bio.terra.tanagra.db.AnnotationValueDao;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.artifact.Annotation;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class AnnotationService {
  private final AnnotationDao annotationDao;
  private final AnnotationValueDao annotationValueDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public AnnotationService(
      AnnotationDao annotationDao,
      AnnotationValueDao annotationValueDao,
      FeatureConfiguration featureConfiguration) {
    this.annotationDao = annotationDao;
    this.annotationValueDao = annotationValueDao;
    this.featureConfiguration = featureConfiguration;
  }

  /** Create a new annotation. */
  public void createAnnotation(
      String studyId, String cohortRevisionGroupId, Annotation annotation) {
    featureConfiguration.artifactStorageEnabledCheck();
    annotationDao.createAnnotation(studyId, cohortRevisionGroupId, annotation);
  }

  /** Delete an existing annotation. */
  public void deleteAnnotation(String studyId, String cohortRevisionGroupId, String annotationId) {
    featureConfiguration.artifactStorageEnabledCheck();
    annotationDao.deleteAnnotation(studyId, cohortRevisionGroupId, annotationId);
  }

  /** Retrieves a list of all annotations for a cohort. */
  public List<Annotation> getAllAnnotations(
      String studyId, String cohortRevisionGroupId, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return annotationDao.getAllAnnotations(studyId, cohortRevisionGroupId, offset, limit);
  }

  /** Retrieves a list of annotations by ID. */
  public List<Annotation> getAnnotations(
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
  public Annotation getAnnotation(
      String studyId, String cohortRevisionGroupId, String annotationId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return annotationDao.getAnnotation(studyId, cohortRevisionGroupId, annotationId);
  }

  /**
   * Update an existing annotation. Currently, can change the annotation's display name or
   * description.
   */
  public Annotation updateAnnotation(
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
  public AnnotationValue createAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      AnnotationValue annotationValue) {
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
  public AnnotationValue updateAnnotationValue(
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

  /** Retrieves a list of all annotation values for a review. */
  public List<AnnotationValue> getAnnotationValues(
      String studyId, String cohortRevisionGroupId, String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return annotationValueDao.getAnnotationValues(studyId, cohortRevisionGroupId, reviewId);
  }

  /**
   * Throw if the annotation value data type does not match the annotation data type or enum values.
   */
  private void validateAnnotationValueDataType(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      Literal annotationValueLiteral) {
    Annotation annotation =
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
