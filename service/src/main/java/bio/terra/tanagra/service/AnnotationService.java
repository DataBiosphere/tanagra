package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.AnnotationDao;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.AnnotationKey;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class AnnotationService {
  private final AnnotationDao annotationDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public AnnotationService(AnnotationDao annotationDao, FeatureConfiguration featureConfiguration) {
    this.annotationDao = annotationDao;
    this.featureConfiguration = featureConfiguration;
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
      ResourceCollection authorizedAnnotationKeyIds,
      String studyId,
      String cohortId,
      int offset,
      int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    if (authorizedAnnotationKeyIds.isAllResources()) {
      return annotationDao.getAllAnnotationKeys(cohortId, offset, limit);
    } else if (authorizedAnnotationKeyIds.isEmpty()) {
      // If the incoming list is empty, the caller does not have permission to see any
      // annotation keys, so we return an empty list.
      return Collections.emptyList();
    } else {
      return annotationDao.getAnnotationKeysMatchingList(
          cohortId,
          authorizedAnnotationKeyIds.getResources().stream()
              .map(ResourceId::getAnnotationKey)
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

  public List<AnnotationValue.Builder> getAllAnnotationValues(String studyId, String cohortId) {
    return annotationDao.getAllAnnotationValues(cohortId);
  }
}
