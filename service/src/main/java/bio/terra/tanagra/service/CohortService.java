package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.CohortDao;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.ActivityLog;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.CohortRevision;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CohortService {
  private final CohortDao cohortDao;
  private final FeatureConfiguration featureConfiguration;
  private final UnderlaysService underlaysService;
  private final StudyService studyService;
  private final ActivityLogService activityLogService;

  @Autowired
  public CohortService(
      CohortDao cohortDao,
      FeatureConfiguration featureConfiguration,
      UnderlaysService underlaysService,
      StudyService studyService,
      ActivityLogService activityLogService) {
    this.cohortDao = cohortDao;
    this.featureConfiguration = featureConfiguration;
    this.underlaysService = underlaysService;
    this.studyService = studyService;
    this.activityLogService = activityLogService;
  }

  /** Create a cohort and its first revision without any criteria. */
  public Cohort createCohort(String studyId, Cohort.Builder cohortBuilder, String userEmail) {
    return createCohort(studyId, cohortBuilder, userEmail, Collections.emptyList());
  }

  /** Create a cohort and its first revision. */
  public Cohort createCohort(
      String studyId,
      Cohort.Builder cohortBuilder,
      String userEmail,
      List<CohortRevision.CriteriaGroupSection> sections) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Make sure underlay name and study id are valid.
    underlaysService.getUnderlay(cohortBuilder.getUnderlay());
    studyService.getStudy(studyId);

    // Create the first revision.
    CohortRevision firstRevision =
        CohortRevision.builder()
            .sections(sections)
            .setIsMostRecent(true)
            .setIsEditable(true)
            .createdBy(userEmail)
            .lastModifiedBy(userEmail)
            .build();
    cohortBuilder.addRevision(firstRevision);

    cohortDao.createCohort(
        studyId, cohortBuilder.createdBy(userEmail).lastModifiedBy(userEmail).build());
    Cohort cohort = cohortDao.getCohort(cohortBuilder.getId());
    activityLogService.logCohort(ActivityLog.Type.CREATE_COHORT, userEmail, studyId, cohort);
    return cohort;
  }

  /** Delete a cohort and all its revisions. */
  public void deleteCohort(String studyId, String cohortId, String userEmail) {
    featureConfiguration.artifactStorageEnabledCheck();
    Cohort cohort = cohortDao.getCohort(cohortId);
    cohortDao.deleteCohort(cohortId);
    activityLogService.logCohort(ActivityLog.Type.DELETE_COHORT, userEmail, studyId, cohort);
  }

  /** List cohorts with their most recent revisions. */
  public List<Cohort> listCohorts(ResourceCollection authorizedCohortIds, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    String studyId = authorizedCohortIds.getParent().getStudy();
    if (authorizedCohortIds.isAllResources()) {
      return cohortDao.getAllCohorts(studyId, offset, limit);
    } else if (authorizedCohortIds.isEmpty()) {
      // If the incoming list is empty, the caller does not have permission to see any
      // cohorts, so we return an empty list.
      return Collections.emptyList();
    } else {
      return cohortDao.getCohortsMatchingList(
          authorizedCohortIds.getResources().stream()
              .map(ResourceId::getCohort)
              .collect(Collectors.toSet()),
          offset,
          limit);
    }
  }

  /** Retrieve a cohort with its most recent revision. */
  public Cohort getCohort(String studyId, String cohortId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return cohortDao.getCohort(cohortId);
  }

  /** Update a cohort's most recent revision. */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Cohort updateCohort(
      String studyId,
      String cohortId,
      String userEmail,
      @Nullable String displayName,
      @Nullable String description,
      @Nullable List<CohortRevision.CriteriaGroupSection> criteriaGroupSections) {
    featureConfiguration.artifactStorageEnabledCheck();
    cohortDao.updateCohort(cohortId, userEmail, displayName, description, criteriaGroupSections);
    return cohortDao.getCohort(cohortId);
  }

  /** @return the id of the frozen revision just created */
  public String createNextRevision(String studyId, String cohortId, String userEmail) {
    return cohortDao.createNextRevision(cohortId, null, userEmail);
  }
}
