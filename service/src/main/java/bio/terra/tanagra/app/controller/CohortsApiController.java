package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE_COHORT;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.STUDY;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.UNDERLAY;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.CohortsApi;
import bio.terra.tanagra.generated.model.ApiCohort;
import bio.terra.tanagra.generated.model.ApiCohortCloneInfo;
import bio.terra.tanagra.generated.model.ApiCohortCountQuery;
import bio.terra.tanagra.generated.model.ApiCohortCreateInfo;
import bio.terra.tanagra.generated.model.ApiCohortList;
import bio.terra.tanagra.generated.model.ApiCohortUpdateInfo;
import bio.terra.tanagra.generated.model.ApiInstanceCountList;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.authentication.UserId;
import bio.terra.tanagra.service.filter.FilterBuilderService;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
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
  private final StudyService studyService;

  @Autowired
  public CohortsApiController(
      CohortService cohortService,
      UnderlayService underlayService,
      FilterBuilderService filterBuilderService,
      AccessControlService accessControlService,
      StudyService studyService) {
    this.cohortService = cohortService;
    this.underlayService = underlayService;
    this.filterBuilderService = filterBuilderService;
    this.accessControlService = accessControlService;
    this.studyService = studyService;
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
                .underlay(body.getUnderlayName())
                .createdBy(SpringAuthentication.getCurrentUser().getEmail()));
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
    accessControlService.checkReadAccess(studyId, List.of(cohortId), List.of());
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
  public ResponseEntity<ApiCohortList> listAllCohorts(
      String userAccessGroup, Integer offset, Integer limit) {
    ResourceCollection authorizedStudyIds =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(STUDY, READ),
            userAccessGroup,
            offset,
            limit);

    List<Study> authorizedStudies =
        studyService.listStudies(authorizedStudyIds, offset, limit, false, null);
    ApiCohortList apiCohorts = new ApiCohortList();
    authorizedStudies.forEach(
        study -> {
          ResourceCollection authorizedCohortIds =
              accessControlService.listAuthorizedResources(
                  SpringAuthentication.getCurrentUser(),
                  Permissions.forActions(COHORT, READ),
                  ResourceId.forStudy(study.getId()),
                  offset,
                  limit);
          cohortService
              .listCohorts(authorizedCohortIds, offset, limit)
              .forEach(cohort -> apiCohorts.add(ToApiUtils.toApiObject(cohort)));
        });
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
            .map(FromApiUtils::fromApiObject)
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
  public ResponseEntity<ApiCohort> cloneCohort(
      String studyId, String cohortId, ApiCohortCloneInfo body) {
    UserId user = SpringAuthentication.getCurrentUser();

    // should have read access to original cohort
    accessControlService.throwIfUnauthorized(
        user, Permissions.forActions(COHORT, READ), ResourceId.forCohort(studyId, cohortId));

    // should have write access to create cohort in destination study
    String destinationStudyId =
        (body.getDestinationStudyId() != null) ? body.getDestinationStudyId() : studyId;
    accessControlService.throwIfUnauthorized(
        user,
        Permissions.forActions(STUDY, CREATE_COHORT),
        ResourceId.forStudy(destinationStudyId));

    Cohort clonedCohort =
        cohortService.cloneCohort(
            studyId,
            cohortId,
            user.getEmail(),
            destinationStudyId,
            body.getDisplayName(),
            body.getDescription());
    return ResponseEntity.ok(ToApiUtils.toApiObject(clonedCohort));
  }

  @Override
  public ResponseEntity<ApiInstanceCountList> queryCohortCounts(
      String studyId, String cohortId, ApiCohortCountQuery body) {
    accessControlService.checkReadAccess(studyId, List.of(cohortId), List.of());
    Cohort cohort = cohortService.getCohort(studyId, cohortId);

    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(UNDERLAY, READ),
        ResourceId.forUnderlay(cohort.getUnderlay()));

    // Build the entity filter.
    EntityFilter cohortFilter;
    if (body.getCriteriaGroupId() != null) {
      cohortFilter =
          filterBuilderService.buildCohortFilterForCriteriaGroup(
              cohort.getUnderlay(),
              cohort
                  .getMostRecentRevision()
                  .getSection(body.getCriteriaGroupSectionId())
                  .getCriteriaGroup(body.getCriteriaGroupId()));
    } else if (body.getCriteriaGroupSectionId() != null) {
      cohortFilter =
          filterBuilderService.buildFilterForCriteriaGroupSection(
              cohort.getUnderlay(),
              cohort.getMostRecentRevision().getSection(body.getCriteriaGroupSectionId()));
    } else {
      cohortFilter =
          filterBuilderService.buildFilterForCohortRevision(
              cohort.getUnderlay(), cohort.getMostRecentRevision());
    }

    // Build the filter for the output entity.
    Underlay underlay = underlayService.getUnderlay(cohort.getUnderlay());
    Entity outputEntity =
        body.getEntity() == null
            ? underlay.getPrimaryEntity()
            : underlay.getEntity(body.getEntity());
    EntityFilter outputEntityFilteredOnCohort =
        filterBuilderService.filterOutputByPrimaryEntity(
            underlay, outputEntity, null, cohortFilter);

    // Run the count query and map the results back to API objects.
    CountQueryResult countQueryResult =
        underlayService.runCountQuery(
            underlay,
            outputEntity,
            body.getCountDistinctAttribute(),
            body.getGroupByAttributes(),
            outputEntityFilteredOnCohort,
            body.getOrderByDirection() == null
                ? OrderByDirection.DESCENDING
                : OrderByDirection.valueOf(body.getOrderByDirection().name()),
            body.getLimit(),
            PageMarker.deserialize(body.getPageMarker()),
            body.getPageSize());
    return ResponseEntity.ok(ToApiUtils.toApiObject(countQueryResult));
  }
}
