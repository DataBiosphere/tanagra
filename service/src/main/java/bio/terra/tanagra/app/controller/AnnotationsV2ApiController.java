package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.ANNOTATION;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT_REVIEW;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.AnnotationsV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.AnnotationService;
import bio.terra.tanagra.service.FromApiConversionService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.Annotation;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import com.google.common.collect.Table;
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
        SpringAuthentication.getCurrentUser(), READ, ANNOTATION, new ResourceId(cohortId));

    Table<String, String, String> latestValues =
        annotationService.getAnnotationValuesForLatestReview(studyId, cohortId);
    String gcsSignedUrl =
        annotationService.writeAnnotationValuesToGcs(studyId, cohortId, latestValues);
    return ResponseEntity.ok(new ApiExportFile().gcsSignedUrl(gcsSignedUrl));
  }

  @Override
  public ResponseEntity<Void> deleteAnnotation(
      String studyId, String cohortId, String annotationId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), DELETE, ANNOTATION, new ResourceId(cohortId));
    annotationService.deleteAnnotation(studyId, cohortId, annotationId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiAnnotationV2> getAnnotation(
      String studyId, String cohortId, String annotationId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), READ, ANNOTATION, new ResourceId(cohortId));
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
        SpringAuthentication.getCurrentUser(), UPDATE, ANNOTATION, new ResourceId(cohortId));
    Annotation updatedAnnotation =
        annotationService.updateAnnotation(
            studyId, cohortId, annotationId, body.getDisplayName(), body.getDescription());
    return ResponseEntity.ok(toApiObject(updatedAnnotation));
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
  public ResponseEntity<ApiAnnotationValueV2> createUpdateAnnotationValue(
      String studyId,
      String cohortId,
      String annotationId,
      String reviewId,
      String valueId,
      ApiLiteralV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, COHORT_REVIEW, new ResourceId(reviewId));

    // Force the annotation value id = entity instance id. Note that this is not a constraint on the
    // internal DB, just one imposed at the API level for the UI's convenience.
    AnnotationValue annotationValueToCreateOrUpdate =
        AnnotationValue.builder()
            .reviewId(reviewId)
            .annotationId(annotationId)
            .annotationValueId(valueId)
            .entityInstanceId(valueId)
            .literal(FromApiConversionService.fromApiObject(body))
            .build();

    AnnotationValue annotationValue =
        annotationService.createUpdateAnnotationValue(
            studyId, cohortId, annotationId, reviewId, annotationValueToCreateOrUpdate);
    return ResponseEntity.ok(ToApiConversionUtils.toApiObject(annotationValue));
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
