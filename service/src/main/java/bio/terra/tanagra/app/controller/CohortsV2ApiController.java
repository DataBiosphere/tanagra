package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.CohortsV2Api;
import bio.terra.tanagra.generated.model.ApiCohortCreateInfoV2;
import bio.terra.tanagra.generated.model.ApiCohortListV2;
import bio.terra.tanagra.generated.model.ApiCohortUpdateInfoV2;
import bio.terra.tanagra.generated.model.ApiCohortV2;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable.LogicalOperator;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.CohortService;
import bio.terra.tanagra.service.StudyService;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Criteria;
import bio.terra.tanagra.service.artifact.CriteriaGroup;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class CohortsV2ApiController implements CohortsV2Api {
  private final StudyService studyService;
  private final CohortService cohortService;
  private final UnderlaysService underlaysService;
  private final AccessControlService accessControlService;

  @Autowired
  public CohortsV2ApiController(
      StudyService studyService,
      CohortService cohortService,
      UnderlaysService underlaysService,
      AccessControlService accessControlService) {
    this.studyService = studyService;
    this.cohortService = cohortService;
    this.underlaysService = underlaysService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiCohortV2> createCohort(String studyId, ApiCohortCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), CREATE, COHORT, new ResourceId(studyId));

    // Make sure underlay name and study id are valid.
    underlaysService.getUnderlay(body.getUnderlayName());
    studyService.getStudy(studyId);

    // Generate random 10-character alphanumeric strings for the new cohort and revision group IDs.
    String newCohortId = RandomStringUtils.randomAlphanumeric(10);
    String newCohortRevisionGroupId = RandomStringUtils.randomAlphanumeric(10);

    Cohort cohortToCreate =
        Cohort.builder()
            .studyId(studyId)
            .cohortId(newCohortId)
            .underlayName(body.getUnderlayName())
            .cohortRevisionGroupId(newCohortRevisionGroupId)
            .version(Cohort.STARTING_VERSION)
            .createdBy(SpringAuthentication.getCurrentUser().getEmail())
            .displayName(body.getDisplayName())
            .description(body.getDescription())
            .build();
    cohortService.createCohort(cohortToCreate);
    return ResponseEntity.ok(
        ToApiConversionUtils.toApiObject(
            cohortService.getCohort(studyId, newCohortRevisionGroupId)));
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
    List<Cohort> authorizedCohorts;
    if (authorizedCohortIds.isAllResourceIds()) {
      authorizedCohorts = cohortService.getAllCohorts(studyId, offset, limit);
    } else {
      authorizedCohorts =
          cohortService.getCohorts(
              studyId,
              authorizedCohortIds.getResourceIds().stream()
                  .map(ResourceId::getId)
                  .collect(Collectors.toList()),
              offset,
              limit);
    }

    ApiCohortListV2 apiCohorts = new ApiCohortListV2();
    authorizedCohorts.stream()
        .forEach(cohort -> apiCohorts.add(ToApiConversionUtils.toApiObject(cohort)));
    return ResponseEntity.ok(apiCohorts);
  }

  @Override
  public ResponseEntity<ApiCohortV2> updateCohort(
      String studyId, String cohortId, ApiCohortUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), UPDATE, COHORT, new ResourceId(cohortId));

    List<CriteriaGroup> criteriaGroups =
        body.getCriteriaGroups() == null
            ? null
            : body.getCriteriaGroups().stream()
                .map(
                    apiCriteriaGroup -> {
                      // Generate random 10-character alphanumeric strings for each criteria group
                      // and criteria (below). We maintain 2 separate IDs (id and user_facing_id) so
                      // that the UI-specified id doesn't need to match the DB schema on the backend
                      // (e.g. for uniqueness). Right now, the API doesn't allow callers to update
                      // individual criteria groups or criteria, just the whole cohort. This means
                      // we can simplify the current implementation and just delete/re-insert all
                      // criteria groups and criteria every time the cohort gets updated, rather
                      // than diffing the criteria groups and updating/inserting/deleting. This is
                      // why the IDs are randomly generated on each update. If we move away from the
                      // simple implementation, then we'll have to expose the internal ids through
                      // the service API.
                      String newCriteriaGroupId = RandomStringUtils.randomAlphanumeric(10);
                      return CriteriaGroup.builder()
                          .criteriaGroupId(newCriteriaGroupId)
                          .userFacingCriteriaGroupId(apiCriteriaGroup.getId())
                          .displayName(apiCriteriaGroup.getDisplayName())
                          .operator(LogicalOperator.valueOf(apiCriteriaGroup.getOperator().name()))
                          .isExcluded(apiCriteriaGroup.isExcluded())
                          .criterias(
                              apiCriteriaGroup.getCriteria().stream()
                                  .map(
                                      apiCriteria -> {
                                        String newCriteriaId =
                                            RandomStringUtils.randomAlphanumeric(10);
                                        return Criteria.builder()
                                            .criteriaGroupId(newCriteriaGroupId)
                                            .criteriaId(newCriteriaId)
                                            .userFacingCriteriaId(apiCriteria.getId())
                                            .displayName(apiCriteria.getDisplayName())
                                            .pluginName(apiCriteria.getPluginName())
                                            .selectionData(apiCriteria.getSelectionData())
                                            .uiConfig(apiCriteria.getUiConfig())
                                            .build();
                                      })
                                  .collect(Collectors.toList()))
                          .build();
                    })
                .collect(Collectors.toList());
    Cohort updatedCohort =
        cohortService.updateCohort(
            studyId, cohortId, body.getDisplayName(), body.getDescription(), criteriaGroups);
    return ResponseEntity.ok(ToApiConversionUtils.toApiObject(updatedCohort));
  }
}
