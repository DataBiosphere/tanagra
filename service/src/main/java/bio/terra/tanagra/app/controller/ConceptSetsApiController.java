package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE_CONCEPT_SET;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.CONCEPT_SET;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;

import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.ConceptSetsApi;
import bio.terra.tanagra.generated.model.ApiConceptSet;
import bio.terra.tanagra.generated.model.ApiConceptSetCreateInfo;
import bio.terra.tanagra.generated.model.ApiConceptSetList;
import bio.terra.tanagra.generated.model.ApiConceptSetUpdateInfo;
import bio.terra.tanagra.generated.model.ApiEntityOutput;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.ConceptSetService;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.artifact.model.Criteria;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ConceptSetsApiController implements ConceptSetsApi {
  private final ConceptSetService conceptSetService;
  private final AccessControlService accessControlService;

  @Autowired
  public ConceptSetsApiController(
      ConceptSetService conceptSetService, AccessControlService accessControlService) {
    this.conceptSetService = conceptSetService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiConceptSet> createConceptSet(
      String studyId, ApiConceptSetCreateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, CREATE_CONCEPT_SET),
        ResourceId.forStudy(studyId));
    ConceptSet createdConceptSet =
        conceptSetService.createConceptSet(
            studyId,
            ConceptSet.builder()
                .displayName(body.getDisplayName())
                .description(body.getDescription())
                .underlay(body.getUnderlayName()),
            SpringAuthentication.getCurrentUser().getEmail());
    return ResponseEntity.ok(ConceptSetsApiController.toApiObject(createdConceptSet));
  }

  @Override
  public ResponseEntity<Void> deleteConceptSet(String studyId, String conceptSetId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(CONCEPT_SET, DELETE),
        ResourceId.forConceptSet(studyId, conceptSetId));
    conceptSetService.deleteConceptSet(studyId, conceptSetId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiConceptSet> getConceptSet(String studyId, String conceptSetId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(CONCEPT_SET, READ),
        ResourceId.forConceptSet(studyId, conceptSetId));
    return ResponseEntity.ok(toApiObject(conceptSetService.getConceptSet(studyId, conceptSetId)));
  }

  @Override
  public ResponseEntity<ApiConceptSetList> listConceptSets(
      String studyId, Integer offset, Integer limit) {
    ResourceCollection authorizedConceptSetIds =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(CONCEPT_SET, READ),
            ResourceId.forStudy(studyId),
            offset,
            limit);
    ApiConceptSetList apiConceptSets = new ApiConceptSetList();
    conceptSetService.listConceptSets(authorizedConceptSetIds, offset, limit).stream()
        .forEach(conceptSet -> apiConceptSets.add(toApiObject(conceptSet)));
    return ResponseEntity.ok(apiConceptSets);
  }

  @Override
  public ResponseEntity<ApiConceptSet> updateConceptSet(
      String studyId, String conceptSetId, ApiConceptSetUpdateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(CONCEPT_SET, UPDATE),
        ResourceId.forConceptSet(studyId, conceptSetId));
    List<Criteria> criteria =
        body.getCriteria() == null
            ? null
            : body.getCriteria().stream()
                .map(FromApiUtils::fromApiObject)
                .collect(Collectors.toList());
    Map<String, List<String>> outputAttributesPerEntity =
        body.getEntityOutputs() == null
            ? null
            : body.getEntityOutputs().stream()
                .collect(Collectors.toMap(eo -> eo.getEntity(), eo -> eo.getExcludeAttributes()));
    ConceptSet updatedConceptSet =
        conceptSetService.updateConceptSet(
            studyId,
            conceptSetId,
            SpringAuthentication.getCurrentUser().getEmail(),
            body.getDisplayName(),
            body.getDescription(),
            criteria,
            outputAttributesPerEntity);
    return ResponseEntity.ok(toApiObject(updatedConceptSet));
  }

  private static ApiConceptSet toApiObject(ConceptSet conceptSet) {
    return new ApiConceptSet()
        .id(conceptSet.getId())
        .underlayName(conceptSet.getUnderlay())
        .displayName(conceptSet.getDisplayName())
        .description(conceptSet.getDescription())
        .created(conceptSet.getCreated())
        .createdBy(conceptSet.getCreatedBy())
        .lastModified(conceptSet.getLastModified())
        .criteria(
            conceptSet.getCriteria() == null
                ? null
                : conceptSet.getCriteria().stream()
                    .map(ToApiUtils::toApiObject)
                    .collect(Collectors.toList()))
        .entityOutputs(
            conceptSet.getExcludeOutputAttributesPerEntity() == null
                ? null
                : conceptSet.getExcludeOutputAttributesPerEntity().entrySet().stream()
                    .map(
                        entry ->
                            new ApiEntityOutput()
                                .entity(entry.getKey())
                                .excludeAttributes(entry.getValue()))
                    .collect(Collectors.toList()));
  }
}
