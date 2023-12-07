package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE_COHORT;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;

import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.CohortsApi;
import bio.terra.tanagra.generated.model.ApiCohort;
import bio.terra.tanagra.generated.model.ApiCohortCreateInfo;
import bio.terra.tanagra.generated.model.ApiCohortList;
import bio.terra.tanagra.generated.model.ApiCohortUpdateInfo;
import bio.terra.tanagra.generated.model.ApiCriteriaGroup;
import bio.terra.tanagra.generated.model.ApiCriteriaGroupSection;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class CohortsApiController implements CohortsApi {
  private final CohortService cohortService;
  private final AccessControlService accessControlService;

  @Autowired
  public CohortsApiController(
      CohortService cohortService, AccessControlService accessControlService) {
    this.cohortService = cohortService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiCohort> createCohort(String studyId, ApiCohortCreateInfo body) {
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
    return ResponseEntity.ok(ToApiUtils.toApiObject(createdCohort));
  }

  @Override
  public ResponseEntity<Void> deleteCohort(
      String studyId, String cohortId, String cohortRevisionId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(COHORT, DELETE),
        ResourceId.forCohort(studyId, cohortId));
    cohortService.deleteCohort(studyId, cohortId, SpringAuthentication.getCurrentUser().getEmail());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiCohort> getCohort(
      String studyId, String cohortId, String cohortRevisionId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(COHORT, READ),
        ResourceId.forCohort(studyId, cohortId));
    return ResponseEntity.ok(ToApiUtils.toApiObject(cohortService.getCohort(studyId, cohortId)));
  }

  @Override
  public ResponseEntity<ApiCohortList> listCohorts(String studyId, Integer offset, Integer limit) {
    ResourceCollection authorizedCohortIds =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(COHORT, READ),
            ResourceId.forStudy(studyId),
            offset,
            limit);
    ApiCohortList apiCohorts = new ApiCohortList();
    cohortService.listCohorts(authorizedCohortIds, offset, limit).stream()
        .forEach(cohort -> apiCohorts.add(ToApiUtils.toApiObject(cohort)));
    return ResponseEntity.ok(apiCohorts);
  }

  @Override
  public ResponseEntity<ApiCohort> updateCohort(
      String studyId, String cohortId, ApiCohortUpdateInfo body, String cohortRevisionId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(COHORT, UPDATE),
        ResourceId.forCohort(studyId, cohortId));
    List<CohortRevision.CriteriaGroupSection> sections =
        body.getCriteriaGroupSections().stream()
            .map(CohortsApiController::fromApiObject)
            .collect(Collectors.toList());
    Cohort updatedCohort =
        cohortService.updateCohort(
            studyId,
            cohortId,
            SpringAuthentication.getCurrentUser().getEmail(),
            body.getDisplayName(),
            body.getDescription(),
            sections);
    return ResponseEntity.ok(ToApiUtils.toApiObject(updatedCohort));
  }

  private static CohortRevision.CriteriaGroupSection fromApiObject(ApiCriteriaGroupSection apiObj) {
    return CohortRevision.CriteriaGroupSection.builder()
        .id(apiObj.getId())
        .displayName(apiObj.getDisplayName())
        .operator(BooleanAndOrFilterVariable.LogicalOperator.valueOf(apiObj.getOperator().name()))
        .setIsExcluded(apiObj.isExcluded())
        .criteriaGroups(
            apiObj.getCriteriaGroups().stream()
                .map(CohortsApiController::fromApiObject)
                .collect(Collectors.toList()))
        .build();
  }

  private static CohortRevision.CriteriaGroup fromApiObject(ApiCriteriaGroup apiObj) {
    return CohortRevision.CriteriaGroup.builder()
        .id(apiObj.getId())
        .displayName(apiObj.getDisplayName())
        .entity(apiObj.getEntity())
        .criteria(
            apiObj.getCriteria().stream()
                .map(FromApiUtils::fromApiObject)
                .collect(Collectors.toList()))
        .build();
  }
}
