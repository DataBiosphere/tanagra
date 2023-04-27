package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.CONCEPT_SET;

import bio.terra.tanagra.app.auth.SpringAuthentication;
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
import bio.terra.tanagra.service.artifact.ConceptSetV1;
import bio.terra.tanagra.service.artifact.CriteriaV1;
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
        SpringAuthentication.getCurrentUser(), CREATE, CONCEPT_SET, new ResourceId(studyId));

    // Make sure underlay name, entity name, and study id are valid.
    underlaysService.getEntity(body.getUnderlayName(), body.getEntity());
    studyService.getStudy(studyId);

    // Generate random 10-character alphanumeric string for the new criteria and concept set IDs.
    String newCriteriaId = RandomStringUtils.randomAlphanumeric(10);
    String newConceptSetId = RandomStringUtils.randomAlphanumeric(10);

    CriteriaV1 criteria =
        body.getCriteria() == null
            ? null
            : CriteriaV1.builder()
                .conceptSetId(newConceptSetId)
                .criteriaId(newCriteriaId)
                .userFacingCriteriaId(body.getCriteria().getId())
                .displayName(body.getCriteria().getDisplayName())
                .pluginName(body.getCriteria().getPluginName())
                .selectionData(body.getCriteria().getSelectionData())
                .uiConfig(body.getCriteria().getUiConfig())
                .build();
    ConceptSetV1 conceptSetToCreate =
        ConceptSetV1.builder()
            .studyId(studyId)
            .conceptSetId(newConceptSetId)
            .underlayName(body.getUnderlayName())
            .entityName(body.getEntity())
            .createdBy(SpringAuthentication.getCurrentUser().getEmail())
            .displayName(body.getDisplayName())
            .description(body.getDescription())
            .criteria(criteria)
            .build();
    conceptSetService.createConceptSet(conceptSetToCreate);
    return ResponseEntity.ok(
        toApiObject(conceptSetService.getConceptSet(studyId, newConceptSetId)));
  }

  @Override
  public ResponseEntity<Void> deleteConceptSet(String studyId, String conceptSetId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), DELETE, CONCEPT_SET, new ResourceId(conceptSetId));
    conceptSetService.deleteConceptSet(studyId, conceptSetId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiConceptSetV2> getConceptSet(String studyId, String conceptSetId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), READ, CONCEPT_SET, new ResourceId(conceptSetId));
    return ResponseEntity.ok(toApiObject(conceptSetService.getConceptSet(studyId, conceptSetId)));
  }

  @Override
  public ResponseEntity<ApiConceptSetListV2> listConceptSets(
      String studyId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedConceptSetIds =
        accessControlService.listResourceIds(
            SpringAuthentication.getCurrentUser(), CONCEPT_SET, offset, limit);
    List<ConceptSetV1> authorizedConceptSets;
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
        .forEach(conceptSet -> apiConceptSets.add(toApiObject(conceptSet)));
    return ResponseEntity.ok(apiConceptSets);
  }

  @Override
  public ResponseEntity<ApiConceptSetV2> updateConceptSet(
      String studyId, String conceptSetId, ApiConceptSetUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, CONCEPT_SET, new ResourceId(conceptSetId));

    // Make sure entity name is valid.
    if (body.getEntity() != null) {
      ConceptSetV1 conceptSet = conceptSetService.getConceptSet(studyId, conceptSetId);
      underlaysService.getEntity(conceptSet.getUnderlayName(), body.getEntity());
    }

    // Generate random 10-character alphanumeric string for the new criteria ID.
    String newCriteriaId = RandomStringUtils.randomAlphanumeric(10);

    CriteriaV1 criteria =
        body.getCriteria() == null
            ? null
            : CriteriaV1.builder()
                .conceptSetId(conceptSetId)
                .criteriaId(newCriteriaId)
                .userFacingCriteriaId(body.getCriteria().getId())
                .displayName(body.getCriteria().getDisplayName())
                .pluginName(body.getCriteria().getPluginName())
                .selectionData(body.getCriteria().getSelectionData())
                .uiConfig(body.getCriteria().getUiConfig())
                .build();
    ConceptSetV1 updatedConceptSet =
        conceptSetService.updateConceptSet(
            studyId,
            conceptSetId,
            body.getEntity(),
            body.getDisplayName(),
            body.getDescription(),
            criteria);
    return ResponseEntity.ok(toApiObject(updatedConceptSet));
  }

  private static ApiConceptSetV2 toApiObject(ConceptSetV1 conceptSet) {
    return new ApiConceptSetV2()
        .id(conceptSet.getConceptSetId())
        .underlayName(conceptSet.getUnderlayName())
        .entity(conceptSet.getEntityName())
        .displayName(conceptSet.getDisplayName())
        .description(conceptSet.getDescription())
        .created(conceptSet.getCreated())
        .createdBy(conceptSet.getCreatedBy())
        .lastModified(conceptSet.getLastModified())
        .criteria(
            conceptSet.getCriteria() == null
                ? null
                : ToApiConversionUtils.toApiObject(conceptSet.getCriteria()));
  }
}
