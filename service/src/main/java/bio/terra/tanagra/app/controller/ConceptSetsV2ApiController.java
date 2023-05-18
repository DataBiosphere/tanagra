package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.CONCEPT_SET;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.ConceptSetsV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.service.*;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.ConceptSet;
import bio.terra.tanagra.service.artifact.Criteria;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ConceptSetsV2ApiController implements ConceptSetsV2Api {
  private final ConceptSetService conceptSetService;
  private final AccessControlService accessControlService;

  @Autowired
  public ConceptSetsV2ApiController(
      ConceptSetService conceptSetService, AccessControlService accessControlService) {
    this.conceptSetService = conceptSetService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiConceptSetV2> createConceptSet(
      String studyId, ApiConceptSetCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), CREATE, CONCEPT_SET, ResourceId.forStudy(studyId));
    Criteria singleCriteria =
        body.getCriteria() == null
            ? null
            : FromApiConversionService.fromApiObject(body.getCriteria());
    ConceptSet createdConceptSet =
        conceptSetService.createConceptSet(
            studyId,
            ConceptSet.builder()
                .displayName(body.getDisplayName())
                .description(body.getDescription())
                .underlay(body.getUnderlayName())
                .entity(body.getEntity())
                .criteria(List.of(singleCriteria)),
            SpringAuthentication.getCurrentUser().getEmail());
    return ResponseEntity.ok(ConceptSetsV2ApiController.toApiObject(createdConceptSet));
  }

  @Override
  public ResponseEntity<Void> deleteConceptSet(String studyId, String conceptSetId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        DELETE,
        CONCEPT_SET,
        ResourceId.forConceptSet(studyId, conceptSetId));
    conceptSetService.deleteConceptSet(studyId, conceptSetId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiConceptSetV2> getConceptSet(String studyId, String conceptSetId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        READ,
        CONCEPT_SET,
        ResourceId.forConceptSet(studyId, conceptSetId));
    return ResponseEntity.ok(toApiObject(conceptSetService.getConceptSet(studyId, conceptSetId)));
  }

  @Override
  public ResponseEntity<ApiConceptSetListV2> listConceptSets(
      String studyId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedConceptSetIds =
        accessControlService.listResourceIds(
            SpringAuthentication.getCurrentUser(),
            CONCEPT_SET,
            ResourceId.forStudy(studyId),
            offset,
            limit);
    ApiConceptSetListV2 apiConceptSets = new ApiConceptSetListV2();
    conceptSetService.listConceptSets(authorizedConceptSetIds, studyId, offset, limit).stream()
        .forEach(conceptSet -> apiConceptSets.add(toApiObject(conceptSet)));
    return ResponseEntity.ok(apiConceptSets);
  }

  @Override
  public ResponseEntity<ApiConceptSetV2> updateConceptSet(
      String studyId, String conceptSetId, ApiConceptSetUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        UPDATE,
        CONCEPT_SET,
        ResourceId.forConceptSet(studyId, conceptSetId));
    Criteria singleCriteria =
        body.getCriteria() == null
            ? null
            : FromApiConversionService.fromApiObject(body.getCriteria());
    ConceptSet updatedConceptSet =
        conceptSetService.updateConceptSet(
            studyId,
            conceptSetId,
            SpringAuthentication.getCurrentUser().getEmail(),
            body.getDisplayName(),
            body.getDescription(),
            body.getEntity(),
            List.of(singleCriteria));
    return ResponseEntity.ok(toApiObject(updatedConceptSet));
  }

  private static ApiConceptSetV2 toApiObject(ConceptSet conceptSet) {
    return new ApiConceptSetV2()
        .id(conceptSet.getId())
        .underlayName(conceptSet.getUnderlay())
        .entity(conceptSet.getEntity())
        .displayName(conceptSet.getDisplayName())
        .description(conceptSet.getDescription())
        .created(conceptSet.getCreated())
        .createdBy(conceptSet.getCreatedBy())
        .lastModified(conceptSet.getLastModified())
        .criteria(
            conceptSet.getCriteria() == null
                ? null
                : ToApiConversionUtils.toApiObject(conceptSet.getCriteria().get(0)));
  }
}
