package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.app.configuration.VersionConfiguration;
import bio.terra.tanagra.db.ActivityLogDao;
import bio.terra.tanagra.service.artifact.*;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogService {
  private final ActivityLogDao activityLogDao;
  private final FeatureConfiguration featureConfiguration;
  private final VersionConfiguration versionConfiguration;

  @Autowired
  public ActivityLogService(
      ActivityLogDao activityLogDao,
      FeatureConfiguration featureConfiguration,
      VersionConfiguration versionConfiguration) {
    this.activityLogDao = activityLogDao;
    this.featureConfiguration = featureConfiguration;
    this.versionConfiguration = versionConfiguration;
  }

  public List<ActivityLog> listActivityLogs(
      int offset,
      int limit,
      @Nullable String userEmailFilter,
      boolean userEmailExactMatch,
      @Nullable ActivityLog.Type activityTypeFilter,
      @Nullable ActivityLogResource.Type resourceTypeFilter) {
    featureConfiguration.activityLogEnabledCheck();
    return activityLogDao.getAllActivityLogs(
        offset,
        limit,
        userEmailFilter,
        userEmailExactMatch,
        activityTypeFilter,
        resourceTypeFilter);
  }

  public ActivityLog getActivityLog(String id) {
    featureConfiguration.activityLogEnabledCheck();
    return activityLogDao.getActivityLog(id);
  }

  public void logStudy(ActivityLog.Type type, String userEmail, Study study) {
    ActivityLogResource resource =
        ActivityLogResource.builder()
            .type(ActivityLogResource.Type.STUDY)
            .studyId(study.getId())
            .build();
    createActivityLog(ActivityLog.builder(), userEmail, type, List.of(resource));
  }

  public void logCohort(ActivityLog.Type type, String userEmail, String studyId, Cohort cohort) {
    ActivityLogResource resource =
        ActivityLogResource.builder()
            .type(ActivityLogResource.Type.COHORT)
            .studyId(studyId)
            .cohortId(cohort.getId())
            .cohortRevisionId(cohort.getMostRecentRevision().getId())
            .build();
    createActivityLog(ActivityLog.builder(), userEmail, type, List.of(resource));
  }

  public void logReview(
      ActivityLog.Type type, String userEmail, String studyId, String cohortId, Review review) {
    ActivityLogResource resource =
        ActivityLogResource.builder()
            .type(ActivityLogResource.Type.REVIEW)
            .studyId(studyId)
            .cohortId(cohortId)
            .reviewId(review.getId())
            .cohortRevisionId(review.getRevision().getId())
            .build();
    createActivityLog(ActivityLog.builder(), userEmail, type, List.of(resource));
  }

  public void logExport(
      String exportModel,
      String userEmail,
      String studyId,
      Map<String, String> cohortToRevisionIdMap) {
    List<ActivityLogResource> resources =
        cohortToRevisionIdMap.entrySet().stream()
            .map(
                entry -> {
                  String cohortId = entry.getKey();
                  String cohortRevisionId = entry.getValue();
                  return ActivityLogResource.builder()
                      .type(ActivityLogResource.Type.COHORT)
                      .studyId(studyId)
                      .cohortId(cohortId)
                      .cohortRevisionId(cohortRevisionId)
                      .build();
                })
            .collect(Collectors.toList());
    createActivityLog(
        ActivityLog.builder().exportModel(exportModel),
        userEmail,
        ActivityLog.Type.EXPORT_COHORT,
        resources);
  }

  private void createActivityLog(
      ActivityLog.Builder activityLogBuilder,
      String userEmail,
      ActivityLog.Type type,
      List<ActivityLogResource> resources) {
    if (featureConfiguration.isActivityLogEnabled()) {
      activityLogDao.createActivityLog(
          activityLogBuilder
              .userEmail(userEmail)
              .type(type)
              .versionGitTag(versionConfiguration.getGitTag())
              .versionGitHash(versionConfiguration.getGitHash())
              .versionBuild(versionConfiguration.getBuild())
              .resources(resources)
              .build());
    }
  }

  @VisibleForTesting
  public void clearAllActivityLogs() {
    featureConfiguration.activityLogEnabledCheck();
    activityLogDao.deleteAllActivityLogs();
  }
}
