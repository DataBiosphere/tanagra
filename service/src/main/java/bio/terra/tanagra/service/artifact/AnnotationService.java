package bio.terra.tanagra.service.artifact;

import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.db.AnnotationDao;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.AnnotationValue;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class AnnotationService {
  private final AnnotationDao annotationDao;

  @Autowired
  public AnnotationService(AnnotationDao annotationDao) {
    this.annotationDao = annotationDao;
  }

  public AnnotationKey createAnnotationKey(
      String studyId, String cohortId, AnnotationKey.Builder annotationKeyBuilder) {
    annotationDao.createAnnotationKey(cohortId, annotationKeyBuilder.build());
    return annotationDao.getAnnotationKey(cohortId, annotationKeyBuilder.getId());
  }

  public void deleteAnnotationKey(String studyId, String cohortId, String annotationKeyId) {
    annotationDao.deleteAnnotationKey(cohortId, annotationKeyId);
  }

  public List<AnnotationKey> listAnnotationKeys(
      ResourceCollection authorizedAnnotationKeyIds, int offset, int limit) {
    String cohortId = authorizedAnnotationKeyIds.getParent().getCohort();
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
    return annotationDao.getAnnotationKey(cohortId, annotationKeyId);
  }

  public AnnotationKey updateAnnotationKey(
      String studyId,
      String cohortId,
      String annotationKeyId,
      @Nullable String displayName,
      @Nullable String description) {
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
    AnnotationKey annotationKey = annotationDao.getAnnotationKey(cohortId, annotationKeyId);
    annotationValues.forEach(annotationKey::validateValue);
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
