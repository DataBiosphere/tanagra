package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.CohortDao;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.CriteriaGroup;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CohortService {
  private final CohortDao cohortDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public CohortService(CohortDao cohortDao, FeatureConfiguration featureConfiguration) {
    this.cohortDao = cohortDao;
    this.featureConfiguration = featureConfiguration;
  }

  /** Create a new cohort, the first revision in a new revision group. */
  public void createCohort(Cohort cohort) {
    featureConfiguration.artifactStorageEnabledCheck();
    cohortDao.createCohortFirstVersion(cohort);
  }

  /** Delete an existing cohort by revision group ID, including all frozen versions. */
  public void deleteCohort(String studyId, String cohortRevisionGroupId) {
    featureConfiguration.artifactStorageEnabledCheck();
    cohortDao.deleteCohortAllVersions(studyId, cohortRevisionGroupId);
  }

  /** Retrieves a list of all most recent cohorts for a study. */
  public List<Cohort> getAllCohorts(String studyId, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return cohortDao.getAllCohortsLatestVersion(studyId, offset, limit);
  }

  /** Retrieves a list of most recent cohorts by ID. */
  public List<Cohort> getCohorts(
      String studyId, List<String> cohortRevisionGroupIds, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return cohortDao.getCohortsMatchingListLatestVersion(
        studyId, new HashSet<>(cohortRevisionGroupIds), offset, limit);
  }

  /** Retrieves a most recent cohort by ID. */
  public Cohort getCohort(String studyId, String cohortRevisionGroupId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return cohortDao.getCohortLatestVersion(studyId, cohortRevisionGroupId);
  }

  /**
   * Update an existing cohort's latest version. Currently, can change the cohort's display name,
   * description, or criteria groups.
   *
   * @param studyId study ID
   * @param cohortRevisionGroupId cohort revision group ID
   * @param displayName name to change - may be null
   * @param description description to change - may be null
   * @param criteriaGroups set of criteria groups to change - may be null
   */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Cohort updateCohort(
      String studyId,
      String cohortRevisionGroupId,
      @Nullable String displayName,
      @Nullable String description,
      @Nullable List<CriteriaGroup> criteriaGroups) {
    featureConfiguration.artifactStorageEnabledCheck();
    cohortDao.updateCohortLatestVersion(
        studyId, cohortRevisionGroupId, displayName, description, criteriaGroups);
    return cohortDao.getCohortLatestVersion(studyId, cohortRevisionGroupId);
  }
}
