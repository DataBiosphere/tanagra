package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.CONCEPT_SET;

import bio.terra.tanagra.generated.controller.ConceptSetsV2Api;
import bio.terra.tanagra.generated.model.ApiConceptSetCreateInfoV2;
import bio.terra.tanagra.generated.model.ApiConceptSetListV2;
import bio.terra.tanagra.generated.model.ApiConceptSetUpdateInfoV2;
import bio.terra.tanagra.generated.model.ApiConceptSetV2;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.ConceptSetService;
import bio.terra.tanagra.service.StudyService;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.ConceptSet;
import bio.terra.tanagra.service.artifact.Criteria;
import bio.terra.tanagra.service.auth.UserId;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ConceptSetsV2ApiController implements ConceptSetsV2Api {
  private final StudyService studyService;
  private final ConceptSetService conceptSetService;
  private final UnderlaysService underlaysService;
  private final AccessControlService accessControlService;

  @Autowired
  public ConceptSetsV2ApiController(
      StudyService studyService,
      ConceptSetService conceptSetService,
      UnderlaysService underlaysService,
      AccessControlService accessControlService) {
    this.studyService = studyService;
    this.conceptSetService = conceptSetService;
    this.underlaysService = underlaysService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiConceptSetV2> createConceptSet(
      String studyId, ApiConceptSetCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        UserId.currentUser(), CREATE, CONCEPT_SET, new ResourceId(studyId));

    // Make sure underlay name, entity name, and study id are valid.
    underlaysService.getEntity(body.getUnderlayName(), body.getEntity());
    studyService.getStudy(studyId);

    // Generate random 10-character alphanumeric string for the new concept set ID.
    String newConceptSetId = RandomStringUtils.randomAlphanumeric(10);

    ConceptSet conceptSetToCreate =
        ConceptSet.builder()
            .studyId(studyId)
            .conceptSetId(newConceptSetId)
            .underlayName(body.getUnderlayName())
            .entityName(body.getEntity())
            .displayName(body.getDisplayName())
            .description(body.getDescription())
            .build();
    conceptSetService.createConceptSet(conceptSetToCreate);
    return ResponseEntity.ok(
        ToApiConversionUtils.toApiObject(
            conceptSetService.getConceptSet(studyId, newConceptSetId)));
  }

  @Override
  public ResponseEntity<Void> deleteConceptSet(String studyId, String conceptSetId) {
    accessControlService.throwIfUnauthorized(
        UserId.currentUser(), DELETE, CONCEPT_SET, new ResourceId(conceptSetId));
    conceptSetService.deleteConceptSet(studyId, conceptSetId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiConceptSetV2> getConceptSet(String studyId, String conceptSetId) {
    accessControlService.throwIfUnauthorized(
        UserId.currentUser(), READ, CONCEPT_SET, new ResourceId(conceptSetId));
    return ResponseEntity.ok(
        ToApiConversionUtils.toApiObject(conceptSetService.getConceptSet(studyId, conceptSetId)));
  }

  @Override
  public ResponseEntity<ApiConceptSetListV2> listConceptSets(
      String studyId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedConceptSetIds =
        accessControlService.listResourceIds(UserId.currentUser(), CONCEPT_SET, offset, limit);
    List<ConceptSet> authorizedConceptSets;
    if (authorizedConceptSetIds.isAllResourceIds()) {
      authorizedConceptSets = conceptSetService.getAllConceptSets(studyId, offset, limit);
    } else {
      authorizedConceptSets =
          conceptSetService.getConceptSets(
              studyId,
              authorizedConceptSetIds.getResourceIds().stream()
                  .map(ResourceId::getId)
                  .collect(Collectors.toList()),
              offset,
              limit);
    }

    ApiConceptSetListV2 apiConceptSets = new ApiConceptSetListV2();
    authorizedConceptSets.stream()
        .forEach(conceptSet -> apiConceptSets.add(ToApiConversionUtils.toApiObject(conceptSet)));
    return ResponseEntity.ok(apiConceptSets);
  }

  @Override
  public ResponseEntity<ApiConceptSetV2> updateConceptSet(
      String studyId, String conceptSetId, ApiConceptSetUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        UserId.currentUser(), UPDATE, CONCEPT_SET, new ResourceId(conceptSetId));

    // Make sure entity name is valid.
    if (body.getEntity() != null) {
      ConceptSet conceptSet = conceptSetService.getConceptSet(studyId, conceptSetId);
      underlaysService.getEntity(conceptSet.getUnderlayName(), body.getEntity());
    }

    // Generate random 10-character alphanumeric string for the new criteria ID.
    String newCriteriaId = RandomStringUtils.randomAlphanumeric(10);

    Criteria criteria =
        body.getCriteria() == null
            ? null
            : Criteria.builder()
                .conceptSetId(conceptSetId)
                .criteriaId(newCriteriaId)
                .userFacingCriteriaId(body.getCriteria().getId())
                .displayName(body.getCriteria().getDisplayName())
                .pluginName(body.getCriteria().getPluginName())
                .selectionData(body.getCriteria().getSelectionData())
                .uiConfig(body.getCriteria().getUiConfig())
                .build();
    ConceptSet updatedConceptSet =
        conceptSetService.updateConceptSet(
            studyId,
            conceptSetId,
            body.getEntity(),
            body.getDisplayName(),
            body.getDescription(),
            criteria);
    return ResponseEntity.ok(ToApiConversionUtils.toApiObject(updatedConceptSet));
  }
}
