package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.AnnotationDao;
import bio.terra.tanagra.service.artifact.Annotation;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnnotationService {
  private final AnnotationDao annotationDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public AnnotationService(AnnotationDao annotationDao, FeatureConfiguration featureConfiguration) {
    this.annotationDao = annotationDao;
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
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
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
}
