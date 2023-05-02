package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.*;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.AnnotationsV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.AnnotationService;
import bio.terra.tanagra.service.FromApiConversionService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.model.AnnotationKey;
import com.google.common.collect.Table;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class AnnotationsV2ApiController implements AnnotationsV2Api {
  private final AnnotationService annotationService;
  private final AccessControlService accessControlService;

  @Autowired
  public AnnotationsV2ApiController(
      AnnotationService annotationService, AccessControlService accessControlService) {
    this.annotationService = annotationService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiAnnotationV2> createAnnotationKey(
      String studyId, String cohortId, ApiAnnotationCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), CREATE, ANNOTATION, new ResourceId(cohortId));
    AnnotationKey createdAnnotationKey =
        annotationService.createAnnotationKey(
            studyId,
            cohortId,
            AnnotationKey.builder()
                .displayName(body.getDisplayName())
                .description(body.getDescription())
                .dataType(Literal.DataType.valueOf(body.getDataType().name()))
                .enumVals(body.getEnumVals()));
    return ResponseEntity.ok(toApiObject(createdAnnotationKey));
  }

  @Override
  public ResponseEntity<Void> deleteAnnotationKey(
      String studyId, String cohortId, String annotationKeyId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), DELETE, ANNOTATION, new ResourceId(cohortId));
    annotationService.deleteAnnotationKey(studyId, cohortId, annotationKeyId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiAnnotationV2> getAnnotationKey(
      String studyId, String cohortId, String annotationKeyId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), READ, ANNOTATION, new ResourceId(cohortId));
    return ResponseEntity.ok(
        toApiObject(annotationService.getAnnotationKey(studyId, cohortId, annotationKeyId)));
  }

  @Override
  public ResponseEntity<ApiAnnotationListV2> listAnnotationKeys(
      String studyId, String cohortId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedAnnotationKeyIds =
        accessControlService.listResourceIds(
            SpringAuthentication.getCurrentUser(), ANNOTATION, offset, limit);
    ApiAnnotationListV2 apiAnnotationKeys = new ApiAnnotationListV2();
    annotationService
        .listAnnotationKeys(authorizedAnnotationKeyIds, studyId, cohortId, offset, limit).stream()
        .forEach(annotationKey -> apiAnnotationKeys.add(toApiObject(annotationKey)));
    return ResponseEntity.ok(apiAnnotationKeys);
  }

  @Override
  public ResponseEntity<ApiAnnotationV2> updateAnnotationKey(
      String studyId, String cohortId, String annotationKeyId, ApiAnnotationUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, ANNOTATION, new ResourceId(cohortId));
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
      ApiLiteralV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, COHORT_REVIEW, new ResourceId(reviewId));
    // The API currently restricts the caller to a single annotation value per review instance per
    // annotation key, but the backend can handle a list.
    annotationService.updateAnnotationValues(
        studyId,
        cohortId,
        annotationKeyId,
        reviewId,
        instanceId,
        List.of(FromApiConversionService.fromApiObject(body)));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> deleteAnnotationValues(
      String studyId, String cohortId, String annotationKeyId, String reviewId, String instanceId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, COHORT_REVIEW, new ResourceId(reviewId));
    annotationService.deleteAnnotationValues(
        studyId, cohortId, annotationKeyId, reviewId, instanceId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiExportFile> exportAnnotationValues(String studyId, String cohortId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), READ, ANNOTATION, new ResourceId(cohortId));

    Table<String, String, String> latestValues =
        annotationService.getAnnotationValuesForLatestReview(studyId, cohortId);
    String gcsSignedUrl =
        annotationService.writeAnnotationValuesToGcs(studyId, cohortId, latestValues);
    return ResponseEntity.ok(new ApiExportFile().gcsSignedUrl(gcsSignedUrl));
  }

  private static ApiAnnotationV2 toApiObject(AnnotationKey annotationKey) {
    return new ApiAnnotationV2()
        .id(annotationKey.getId())
        .displayName(annotationKey.getDisplayName())
        .description(annotationKey.getDescription())
        .dataType(ApiDataTypeV2.fromValue(annotationKey.getDataType().name()))
        .enumVals(annotationKey.getEnumVals());
  }
}
