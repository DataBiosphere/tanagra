package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE_ANNOTATION_KEY;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.ANNOTATION_KEY;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.REVIEW;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.generated.controller.AnnotationsApi;
import bio.terra.tanagra.generated.model.ApiAnnotation;
import bio.terra.tanagra.generated.model.ApiAnnotationCreateInfo;
import bio.terra.tanagra.generated.model.ApiAnnotationList;
import bio.terra.tanagra.generated.model.ApiAnnotationUpdateInfo;
import bio.terra.tanagra.generated.model.ApiDataType;
import bio.terra.tanagra.generated.model.ApiLiteral;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol2.AccessControl2Service;
import bio.terra.tanagra.service.artifact.AnnotationService;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class AnnotationsApiController implements AnnotationsApi {
  private final AnnotationService annotationService;
  private final AccessControl2Service accessControlService;

  @Autowired
  public AnnotationsApiController(
      AnnotationService annotationService, AccessControl2Service accessControlService) {
    this.annotationService = annotationService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiAnnotation> createAnnotationKey(
      String studyId, String cohortId, ApiAnnotationCreateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(COHORT, CREATE_ANNOTATION_KEY),
        ResourceId.forCohort(studyId, cohortId));
    AnnotationKey createdAnnotationKey =
        annotationService.createAnnotationKey(
            studyId,
            cohortId,
            AnnotationKey.builder()
                .displayName(body.getDisplayName())
                .description(body.getDescription())
                .dataType(DataType.valueOf(body.getDataType().name()))
                .enumVals(body.getEnumVals()));
    return ResponseEntity.ok(toApiObject(createdAnnotationKey));
  }

  @Override
  public ResponseEntity<Void> deleteAnnotationKey(
      String studyId, String cohortId, String annotationKeyId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(ANNOTATION_KEY, DELETE),
        ResourceId.forAnnotationKey(studyId, cohortId, annotationKeyId));
    annotationService.deleteAnnotationKey(studyId, cohortId, annotationKeyId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiAnnotation> getAnnotationKey(
      String studyId, String cohortId, String annotationKeyId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(ANNOTATION_KEY, READ),
        ResourceId.forAnnotationKey(studyId, cohortId, annotationKeyId));
    return ResponseEntity.ok(
        toApiObject(annotationService.getAnnotationKey(studyId, cohortId, annotationKeyId)));
  }

  @Override
  public ResponseEntity<ApiAnnotationList> listAnnotationKeys(
      String studyId, String cohortId, Integer offset, Integer limit) {
    ResourceCollection authorizedAnnotationKeyIds =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(ANNOTATION_KEY, READ),
            ResourceId.forCohort(studyId, cohortId),
            offset,
            limit);
    ApiAnnotationList apiAnnotationKeys = new ApiAnnotationList();
    annotationService
        .listAnnotationKeys(authorizedAnnotationKeyIds, offset, limit)
        .forEach(annotationKey -> apiAnnotationKeys.add(toApiObject(annotationKey)));
    return ResponseEntity.ok(apiAnnotationKeys);
  }

  @Override
  public ResponseEntity<ApiAnnotation> updateAnnotationKey(
      String studyId, String cohortId, String annotationKeyId, ApiAnnotationUpdateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(ANNOTATION_KEY, UPDATE),
        ResourceId.forAnnotationKey(studyId, cohortId, annotationKeyId));
    AnnotationKey updatedAnnotationKey =
        annotationService.updateAnnotationKey(
            studyId, cohortId, annotationKeyId, body.getDisplayName(), body.getDescription());
    return ResponseEntity.ok(toApiObject(updatedAnnotationKey));
  }

  @Override
  public ResponseEntity<Void> updateAnnotationValue(
      String studyId,
      String cohortId,
      String annotationKeyId,
      String reviewId,
      String instanceId,
      ApiLiteral body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(REVIEW, UPDATE),
        ResourceId.forReview(studyId, cohortId, reviewId));
    // The API currently restricts the caller to a single annotation value per review instance per
    // annotation key, but the backend can handle a list.
    annotationService.updateAnnotationValues(
        studyId,
        cohortId,
        annotationKeyId,
        reviewId,
        instanceId,
        List.of(FromApiUtils.fromApiObject(body)));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> deleteAnnotationValues(
      String studyId, String cohortId, String annotationKeyId, String reviewId, String instanceId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(REVIEW, UPDATE),
        ResourceId.forReview(studyId, cohortId, reviewId));
    annotationService.deleteAnnotationValues(
        studyId, cohortId, annotationKeyId, reviewId, instanceId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private static ApiAnnotation toApiObject(AnnotationKey annotationKey) {
    return new ApiAnnotation()
        .id(annotationKey.getId())
        .displayName(annotationKey.getDisplayName())
        .description(annotationKey.getDescription())
        .dataType(ApiDataType.fromValue(annotationKey.getDataType().name()))
        .enumVals(annotationKey.getEnumVals());
  }
}
