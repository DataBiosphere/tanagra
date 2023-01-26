package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.ANNOTATION;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT_REVIEW;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.AnnotationsV2Api;
import bio.terra.tanagra.generated.model.ApiAnnotationCreateInfoV2;
import bio.terra.tanagra.generated.model.ApiAnnotationListV2;
import bio.terra.tanagra.generated.model.ApiAnnotationUpdateInfoV2;
import bio.terra.tanagra.generated.model.ApiAnnotationV2;
import bio.terra.tanagra.generated.model.ApiAnnotationValueCreateUpdateInfoV2;
import bio.terra.tanagra.generated.model.ApiAnnotationValueV2;
import bio.terra.tanagra.generated.model.ApiDataTypeV2;
import bio.terra.tanagra.generated.model.ApiExportFile;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.AnnotationService;
import bio.terra.tanagra.service.FromApiConversionService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.Annotation;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
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
  public ResponseEntity<ApiAnnotationV2> createAnnotation(
      String studyId, String cohortId, ApiAnnotationCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), CREATE, ANNOTATION, new ResourceId(cohortId));

    // Generate a random 10-character alphanumeric string for the new annotation ID.
    String newAnnotationId = RandomStringUtils.randomAlphanumeric(10);

    Annotation annotationToCreate =
        Annotation.builder()
            .annotationId(newAnnotationId)
            .displayName(body.getDisplayName())
            .description(body.getDescription())
            .dataType(Literal.DataType.valueOf(body.getDataType().toString()))
            .enumVals(body.getEnumVals())
            .build();

    annotationService.createAnnotation(studyId, cohortId, annotationToCreate);
    return ResponseEntity.ok(
        toApiObject(annotationService.getAnnotation(studyId, cohortId, newAnnotationId)));
  }

  @Override
  public ResponseEntity<ApiExportFile> exportAnnotationValues(String studyId, String cohortId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), READ, COHORT, new ResourceId(cohortId));
    Collection<AnnotationValue> values =
        annotationService.getAnnotationValuesForLatestReview(studyId, cohortId);
    String gcsSignedUrl = annotationService.writeAnnotationValuesToGcs(values);
    return ResponseEntity.ok(new ApiExportFile().gcsSignedUrl(gcsSignedUrl));
  }

  @Override
  public ResponseEntity<Void> deleteAnnotation(
      String studyId, String cohortId, String annotationId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), DELETE, ANNOTATION, new ResourceId(annotationId));
    annotationService.deleteAnnotation(studyId, cohortId, annotationId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiAnnotationV2> getAnnotation(
      String studyId, String cohortId, String annotationId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), READ, ANNOTATION, new ResourceId(annotationId));
    return ResponseEntity.ok(
        toApiObject(annotationService.getAnnotation(studyId, cohortId, annotationId)));
  }

  @Override
  public ResponseEntity<ApiAnnotationListV2> listAnnotations(
      String studyId, String cohortId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedAnnotationIds =
        accessControlService.listResourceIds(
            SpringAuthentication.getCurrentUser(), ANNOTATION, offset, limit);
    List<Annotation> authorizedAnnotations;
    if (authorizedAnnotationIds.isAllResourceIds()) {
      authorizedAnnotations = annotationService.getAllAnnotations(studyId, cohortId, offset, limit);
    } else {
      authorizedAnnotations =
          annotationService.getAnnotations(
              studyId,
              cohortId,
              authorizedAnnotationIds.getResourceIds().stream()
                  .map(ResourceId::getId)
                  .collect(Collectors.toList()),
              offset,
              limit);
    }

    ApiAnnotationListV2 apiAnnotations = new ApiAnnotationListV2();
    authorizedAnnotations.stream()
        .forEach(
            annotation -> {
              apiAnnotations.add(toApiObject(annotation));
            });
    return ResponseEntity.ok(apiAnnotations);
  }

  @Override
  public ResponseEntity<ApiAnnotationV2> updateAnnotation(
      String studyId, String cohortId, String annotationId, ApiAnnotationUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, ANNOTATION, new ResourceId(annotationId));
    Annotation updatedAnnotation =
        annotationService.updateAnnotation(
            studyId, cohortId, annotationId, body.getDisplayName(), body.getDescription());
    return ResponseEntity.ok(toApiObject(updatedAnnotation));
  }

  @Override
  public ResponseEntity<ApiAnnotationValueV2> createAnnotationValue(
      String studyId,
      String cohortId,
      String annotationId,
      String reviewId,
      ApiAnnotationValueCreateUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, COHORT_REVIEW, new ResourceId(reviewId));

    // Generate a random 10-character alphanumeric string for the new annotation value ID.
    String newAnnotationValueId = RandomStringUtils.randomAlphanumeric(10);

    AnnotationValue annotationValueToCreate =
        AnnotationValue.builder()
            .reviewId(reviewId)
            .annotationId(annotationId)
            .annotationValueId(newAnnotationValueId)
            .entityInstanceId(body.getInstanceId())
            .literal(FromApiConversionService.fromApiObject(body.getValue()))
            .build();

    AnnotationValue createdValue =
        annotationService.createAnnotationValue(
            studyId, cohortId, annotationId, reviewId, annotationValueToCreate);
    return ResponseEntity.ok(ToApiConversionUtils.toApiObject(createdValue));
  }

  @Override
  public ResponseEntity<Void> deleteAnnotationValue(
      String studyId, String cohortId, String annotationId, String reviewId, String valueId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, COHORT_REVIEW, new ResourceId(reviewId));
    annotationService.deleteAnnotationValue(studyId, cohortId, annotationId, reviewId, valueId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiAnnotationValueV2> updateAnnotationValue(
      String studyId,
      String cohortId,
      String annotationId,
      String reviewId,
      String valueId,
      ApiAnnotationValueCreateUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, COHORT_REVIEW, new ResourceId(reviewId));
    AnnotationValue updatedAnnotationValue =
        annotationService.updateAnnotationValue(
            studyId,
            cohortId,
            annotationId,
            reviewId,
            valueId,
            FromApiConversionService.fromApiObject(body.getValue()));
    return ResponseEntity.ok(ToApiConversionUtils.toApiObject(updatedAnnotationValue));
  }

  private static ApiAnnotationV2 toApiObject(Annotation annotation) {
    return new ApiAnnotationV2()
        .id(annotation.getAnnotationId())
        .displayName(annotation.getDisplayName())
        .description(annotation.getDescription())
        .dataType(ApiDataTypeV2.fromValue(annotation.getDataType().name()))
        .enumVals(annotation.getEnumVals());
  }
}
