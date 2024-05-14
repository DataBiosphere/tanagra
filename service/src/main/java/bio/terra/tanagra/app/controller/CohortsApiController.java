package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE_COHORT;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.CohortsApi;
import bio.terra.tanagra.generated.model.ApiCohort;
import bio.terra.tanagra.generated.model.ApiCohortCountQuery;
import bio.terra.tanagra.generated.model.ApiCohortCreateInfo;
import bio.terra.tanagra.generated.model.ApiCohortList;
import bio.terra.tanagra.generated.model.ApiCohortUpdateInfo;
import bio.terra.tanagra.generated.model.ApiCriteriaGroup;
import bio.terra.tanagra.generated.model.ApiCriteriaGroupSection;
import bio.terra.tanagra.generated.model.ApiInstanceCountList;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.filter.FilterBuilderService;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class CohortsApiController implements CohortsApi {
  private final CohortService cohortService;
  private final UnderlayService underlayService;
  private final FilterBuilderService filterBuilderService;
  private final AccessControlService accessControlService;

  @Autowired
  public CohortsApiController(
      CohortService cohortService,
      UnderlayService underlayService,
      FilterBuilderService filterBuilderService,
      AccessControlService accessControlService) {
    this.cohortService = cohortService;
    this.underlayService = underlayService;
    this.filterBuilderService = filterBuilderService;
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
    cohortService
        .listCohorts(authorizedCohortIds, offset, limit)
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

  @Override
  public ResponseEntity<ApiInstanceCountList> queryCohortCounts(
      String studyId, String cohortId, ApiCohortCountQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(COHORT, READ),
        ResourceId.forCohort(studyId, cohortId));
    Cohort cohort = cohortService.getCohort(studyId, cohortId);

    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(cohort.getUnderlay()));

    // Build the entity filter.
    EntityFilter filter;
    if (body.getCriteriaGroupId() != null) {
      filter =
          filterBuilderService.buildFilterForCriteriaGroup(
              cohort.getUnderlay(),
              cohort
                  .getMostRecentRevision()
                  .getSection(body.getCriteriaGroupSectionId())
                  .getCriteriaGroup(body.getCriteriaGroupId()));
    } else if (body.getCriteriaGroupSectionId() != null) {
      filter =
          filterBuilderService.buildFilterForCriteriaGroupSection(
              cohort.getUnderlay(),
              cohort.getMostRecentRevision().getSection(body.getCriteriaGroupSectionId()));
    } else {
      filter =
          filterBuilderService.buildFilterForCohortRevision(
              cohort.getUnderlay(), cohort.getMostRecentRevision());
    }

    // Run the count query and map the results back to API objects.
    Underlay underlay = underlayService.getUnderlay(cohort.getUnderlay());
    CountQueryResult countQueryResult =
        underlayService.runCountQuery(
            underlay,
            underlay.getPrimaryEntity(),
            body.getAttributes() == null ? List.of() : body.getAttributes(),
            filter,
            PageMarker.deserialize(body.getPageMarker()),
            body.getPageSize());
    return ResponseEntity.ok(ToApiUtils.toApiObject(countQueryResult));
  }

  private static CohortRevision.CriteriaGroupSection fromApiObject(ApiCriteriaGroupSection apiObj) {
    return CohortRevision.CriteriaGroupSection.builder()
        .id(apiObj.getId())
        .displayName(apiObj.getDisplayName())
        .operator(BooleanAndOrFilter.LogicalOperator.valueOf(apiObj.getOperator().name()))
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
        .criteria(
            apiObj.getCriteria().stream()
                .map(FromApiUtils::fromApiObject)
                .collect(Collectors.toList()))
        .build();
  }
}
