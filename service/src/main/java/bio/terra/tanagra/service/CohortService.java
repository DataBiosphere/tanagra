package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.CohortDao;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
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

  @Autowired
  public CohortService(
      CohortDao cohortDao,
      FeatureConfiguration featureConfiguration,
      UnderlaysService underlaysService,
      StudyService studyService) {
    this.cohortDao = cohortDao;
    this.featureConfiguration = featureConfiguration;
    this.underlaysService = underlaysService;
    this.studyService = studyService;
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
    return cohortDao.getCohort(cohortBuilder.getId());
  }

  /** Delete a cohort and all its revisions. */
  public void deleteCohort(String studyId, String cohortId) {
    featureConfiguration.artifactStorageEnabledCheck();
    cohortDao.deleteCohort(cohortId);
  }

  /** List cohorts with their most recent revisions. */
  public List<Cohort> listCohorts(
      ResourceIdCollection authorizedCohortIds, String studyId, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    if (authorizedCohortIds.isAllResourceIds()) {
      return cohortDao.getAllCohorts(studyId, offset, limit);
    } else {
      return cohortDao.getCohortsMatchingList(
          authorizedCohortIds.getResourceIds().stream()
              .map(ResourceId::getId)
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
}
