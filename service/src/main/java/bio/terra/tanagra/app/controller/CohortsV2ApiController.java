package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.CohortsV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.CohortService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.CohortRevision;
import bio.terra.tanagra.service.model.Criteria;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class CohortsV2ApiController implements CohortsV2Api {
  private final CohortService cohortService;
  private final AccessControlService accessControlService;

  @Autowired
  public CohortsV2ApiController(
      CohortService cohortService, AccessControlService accessControlService) {
    this.cohortService = cohortService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiCohortV2> createCohort(String studyId, ApiCohortCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), CREATE, COHORT, new ResourceId(studyId));
    Cohort createdCohort =
        cohortService.createCohort(
            studyId,
            Cohort.builder()
                .createdBy(SpringAuthentication.getCurrentUser().getEmail())
                .displayName(body.getDisplayName())
                .description(body.getDescription())
                .underlayName(body.getUnderlayName()));
    return ResponseEntity.ok(ToApiConversionUtils.toApiObject(createdCohort));
  }

  @Override
  public ResponseEntity<Void> deleteCohort(String studyId, String cohortId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), DELETE, COHORT, new ResourceId(cohortId));
    cohortService.deleteCohort(studyId, cohortId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiCohortV2> getCohort(String studyId, String cohortId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), READ, COHORT, new ResourceId(cohortId));
    return ResponseEntity.ok(
        ToApiConversionUtils.toApiObject(cohortService.getCohort(studyId, cohortId)));
  }

  @Override
  public ResponseEntity<ApiCohortListV2> listCohorts(
      String studyId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedCohortIds =
        accessControlService.listResourceIds(
            SpringAuthentication.getCurrentUser(), COHORT, offset, limit);
    ApiCohortListV2 apiCohorts = new ApiCohortListV2();
    cohortService.listCohorts(authorizedCohortIds, studyId, offset, limit).stream()
        .forEach(cohort -> apiCohorts.add(ToApiConversionUtils.toApiObject(cohort)));
    return ResponseEntity.ok(apiCohorts);
  }

  @Override
  public ResponseEntity<ApiCohortV2> updateCohort(
      String studyId, String cohortId, ApiCohortUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, COHORT, new ResourceId(cohortId));
    List<CohortRevision.CriteriaGroupSection> sections =
        body.getCriteriaGroupSections().stream()
            .map(CohortsV2ApiController::fromApiObject)
            .collect(Collectors.toList());
    Cohort updatedCohort =
        cohortService.updateCohort(
            studyId,
            cohortId,
            SpringAuthentication.getCurrentUser().getEmail(),
            body.getDisplayName(),
            body.getDescription(),
            sections);
    return ResponseEntity.ok(ToApiConversionUtils.toApiObject(updatedCohort));
  }

  private static CohortRevision.CriteriaGroupSection fromApiObject(
      ApiCriteriaGroupSectionV3 apiObj) {
    return CohortRevision.CriteriaGroupSection.builder()
        .id(apiObj.getId())
        .displayName(apiObj.getDisplayName())
        .operator(BooleanAndOrFilterVariable.LogicalOperator.valueOf(apiObj.getOperator().name()))
        .setIsExcluded(apiObj.isExcluded())
        .criteriaGroups(
            apiObj.getCriteriaGroups().stream()
                .map(CohortsV2ApiController::fromApiObject)
                .collect(Collectors.toList()))
        .build();
  }

  private static CohortRevision.CriteriaGroup fromApiObject(ApiCriteriaGroupV3 apiObj) {
    return CohortRevision.CriteriaGroup.builder()
        .id(apiObj.getId())
        .displayName(apiObj.getDisplayName())
        .entity(apiObj.getEntity())
        .groupByCountOperator(
            BinaryFilterVariable.BinaryOperator.valueOf(apiObj.getGroupByCountOperator().name()))
        .groupByCountValue(apiObj.getGroupByCountValue())
        .criteria(
            apiObj.getCriteria().stream()
                .map(CohortsV2ApiController::fromApiObject)
                .collect(Collectors.toList()))
        .build();
  }

  private static Criteria fromApiObject(ApiCriteriaV2 apiObj) {
    return Criteria.builder()
        .id(apiObj.getId())
        .displayName(apiObj.getDisplayName())
        .pluginName(apiObj.getPluginName())
        .uiConfig(apiObj.getUiConfig())
        .selectionData(apiObj.getSelectionData())
        .tags(apiObj.getTags())
        .build();
  }
}
