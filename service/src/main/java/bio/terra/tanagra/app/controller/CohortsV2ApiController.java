package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.*;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.CohortsV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.CohortService;
import bio.terra.tanagra.service.FromApiConversionService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.CohortRevision;
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
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(STUDY, CREATE_COHORT),
        ResourceId.forStudy(studyId));
    Cohort createdCohort =
        cohortService.createCohort(
            studyId,
            Cohort.builder()
                .displayName(body.getDisplayName())
                .description(body.getDescription())
                .underlay(body.getUnderlayName()),
            SpringAuthentication.getCurrentUser().getEmail());
    return ResponseEntity.ok(ToApiConversionUtils.toApiObject(createdCohort));
  }

  @Override
  public ResponseEntity<Void> deleteCohort(String studyId, String cohortId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(COHORT, DELETE),
        ResourceId.forCohort(studyId, cohortId));
    cohortService.deleteCohort(studyId, cohortId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiCohortV2> getCohort(String studyId, String cohortId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(COHORT, READ),
        ResourceId.forCohort(studyId, cohortId));
    return ResponseEntity.ok(
        ToApiConversionUtils.toApiObject(cohortService.getCohort(studyId, cohortId)));
  }

  @Override
  public ResponseEntity<ApiCohortListV2> listCohorts(
      String studyId, Integer offset, Integer limit) {
    ResourceCollection authorizedCohortIds =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(COHORT, READ),
            ResourceId.forStudy(studyId),
            offset,
            limit);
    ApiCohortListV2 apiCohorts = new ApiCohortListV2();
    cohortService.listCohorts(authorizedCohortIds, offset, limit).stream()
        .forEach(cohort -> apiCohorts.add(ToApiConversionUtils.toApiObject(cohort)));
    return ResponseEntity.ok(apiCohorts);
  }

  @Override
  public ResponseEntity<ApiCohortV2> updateCohort(
      String studyId, String cohortId, ApiCohortUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(COHORT, UPDATE),
        ResourceId.forCohort(studyId, cohortId));
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
            apiObj.getGroupByCountOperator() == null
                ? null
                : BinaryFilterVariable.BinaryOperator.valueOf(
                    apiObj.getGroupByCountOperator().name()))
        .groupByCountValue(apiObj.getGroupByCountValue())
        .criteria(
            apiObj.getCriteria().stream()
                .map(FromApiConversionService::fromApiObject)
                .collect(Collectors.toList()))
        .build();
  }
}
