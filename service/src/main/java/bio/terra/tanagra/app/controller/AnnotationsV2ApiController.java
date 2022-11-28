package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.ANNOTATION;

import bio.terra.tanagra.generated.controller.AnnotationsV2Api;
import bio.terra.tanagra.generated.model.ApiAnnotationCreateInfoV2;
import bio.terra.tanagra.generated.model.ApiAnnotationListV2;
import bio.terra.tanagra.generated.model.ApiAnnotationUpdateInfoV2;
import bio.terra.tanagra.generated.model.ApiAnnotationV2;
import bio.terra.tanagra.generated.model.ApiDataTypeV2;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.AnnotationService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.Annotation;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
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
    accessControlService.throwIfUnauthorized(null, CREATE, ANNOTATION, new ResourceId(cohortId));

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
  public ResponseEntity<Void> deleteAnnotation(
      String studyId, String cohortId, String annotationId) {
    accessControlService.throwIfUnauthorized(
        null, DELETE, ANNOTATION, new ResourceId(annotationId));
    annotationService.deleteAnnotation(studyId, cohortId, annotationId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiAnnotationV2> getAnnotation(
      String studyId, String cohortId, String annotationId) {
    accessControlService.throwIfUnauthorized(null, READ, ANNOTATION, new ResourceId(annotationId));
    return ResponseEntity.ok(
        toApiObject(annotationService.getAnnotation(studyId, cohortId, annotationId)));
  }

  @Override
  public ResponseEntity<ApiAnnotationListV2> listAnnotations(
      String studyId, String cohortId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedAnnotationIds =
        accessControlService.listResourceIds(ANNOTATION, offset, limit);
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
        null, UPDATE, ANNOTATION, new ResourceId(annotationId));
    Annotation updatedAnnotation =
        annotationService.updateAnnotation(
            studyId, cohortId, annotationId, body.getDisplayName(), body.getDescription());
    return ResponseEntity.ok(toApiObject(updatedAnnotation));
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
