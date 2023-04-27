package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.CohortDao;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.CohortRevision;
import java.util.Collections;
import java.util.HashSet;
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

  public Cohort createCohort(String studyId, Cohort.Builder cohortBuilder) {
    return createCohort(studyId, cohortBuilder, Collections.emptyList());
  }

  public Cohort createCohort(
      String studyId,
      Cohort.Builder cohortBuilder,
      List<CohortRevision.CriteriaGroupSection> sections) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Make sure underlay name and study id are valid.
    underlaysService.getUnderlay(cohortBuilder.getUnderlayName());
    studyService.getStudy(studyId);

    // Create the first revision.
    CohortRevision firstRevision =
        CohortRevision.builder()
            .sections(sections)
            .setIsMostRecent(true)
            .setIsEditable(true)
            .build();
    cohortBuilder.addRevision(firstRevision);

    cohortDao.createCohort(studyId, cohortBuilder.build());
    return cohortDao.getCohort(cohortBuilder.getId());
  }

  /** Delete a cohort and all its revisions. */
  public void deleteCohort(String studyId, String cohortId) {
    featureConfiguration.artifactStorageEnabledCheck();
    cohortDao.deleteCohort(cohortId);
  }

  public List<Cohort> listCohorts(
      ResourceIdCollection authorizedCohortIds, String studyId, int offset, int limit) {
    if (authorizedCohortIds.isAllResourceIds()) {
      return getAllCohorts(studyId, offset, limit);
    } else {
      return getCohorts(
          studyId,
          authorizedCohortIds.getResourceIds().stream()
              .map(ResourceId::getId)
              .collect(Collectors.toList()),
          offset,
          limit);
    }
  }

  /** Retrieve a list of all cohorts. */
  private List<Cohort> getAllCohorts(String studyId, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return cohortDao.getAllCohorts(studyId, offset, limit);
  }

  /** Retrieve a list of cohorts that match the specified IDs. */
  private List<Cohort> getCohorts(String studyId, List<String> cohortIds, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return cohortDao.getCohortsMatchingList(new HashSet<>(cohortIds), offset, limit);
  }

  /** Retrieve a cohort with the latest revision. */
  public Cohort getCohort(String studyId, String cohortId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return cohortDao.getCohort(cohortId);
  }

  /**
   * Update an existing cohort's latest version. Currently, can change the cohort's display name,
   * description, or criteria groups.
   *
   * @param studyId study ID
   * @param cohortId cohort ID
   * @param displayName name to change - may be null
   * @param description description to change - may be null
   * @param criteriaGroupSections set of criteria group sections to change - may be null
   */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Cohort updateCohort(
      String studyId,
      String cohortId,
      String lastModifiedBy,
      @Nullable String displayName,
      @Nullable String description,
      @Nullable List<CohortRevision.CriteriaGroupSection> criteriaGroupSections) {
    featureConfiguration.artifactStorageEnabledCheck();
    cohortDao.updateCohort(
        cohortId, lastModifiedBy, displayName, description, criteriaGroupSections);
    return cohortDao.getCohort(cohortId);
  }
}
