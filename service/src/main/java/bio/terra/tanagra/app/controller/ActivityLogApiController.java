package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.app.configuration.VersionConfiguration;
import bio.terra.tanagra.generated.controller.ActivityLogApi;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.service.ActivityLogService;
import bio.terra.tanagra.service.artifact.ActivityLog;
import bio.terra.tanagra.service.artifact.ActivityLogResource;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

public class ActivityLogApiController implements ActivityLogApi {

  private final ActivityLogService activityLogService;

  @Autowired
  public ActivityLogApiController(ActivityLogService activityLogService) {
    this.activityLogService = activityLogService;
  }

  @Override
  public ResponseEntity<ApiActivityLogEntryList> listActivityLogEntries(
      String userEmail,
      Boolean exactMatch,
      ApiResourceType resourceType,
      ApiActivityType activityType,
      Integer offset,
      Integer limit) {
    // TODO: Add access control call here.
    ApiActivityLogEntryList apiActivityLogs = new ApiActivityLogEntryList();
    activityLogService
        .listActivityLogs(
            offset,
            limit,
            userEmail,
            exactMatch,
            ActivityLog.Type.valueOf(activityType.name()),
            ActivityLogResource.Type.valueOf(resourceType.name()))
        .stream()
        .forEach(activityLog -> apiActivityLogs.add(toApiObject(activityLog)));
    return ResponseEntity.ok(apiActivityLogs);
  }

  @Override
  public ResponseEntity<ApiActivityLogEntry> getActivityLogEntry(String activityLogEntryId) {
    // TODO: Add access control call here.
    return ResponseEntity.ok(toApiObject(activityLogService.getActivityLog(activityLogEntryId)));
  }

  private ApiActivityLogEntry toApiObject(ActivityLog activityLog) {
    return new ApiActivityLogEntry()
        .id(activityLog.getId())
        .userEmail(activityLog.getUserEmail())
        .logged(activityLog.getLogged())
        .systemVersion(
            new ApiSystemVersionV2()
                .gitTag(activityLog.getVersionGitTag())
                .gitHash(activityLog.getVersionGitHash())
                .github(VersionConfiguration.getGithubUrl(activityLog.getVersionGitHash()))
                .build(activityLog.getVersionBuild()))
        .activityType(ApiActivityType.valueOf(activityLog.getType().name()))
        .resources(
            activityLog.getResources().stream()
                .map(alr -> toApiObject(alr))
                .collect(Collectors.toList()))
        .additionalInfo(
            new ApiActivityLogEntryAdditionalInfo().exportModel(activityLog.getExportModel()));
  }

  private ApiResource toApiObject(ActivityLogResource activityLogResource) {
    return new ApiResource()
        .type(ApiResourceType.valueOf(activityLogResource.getType().name()))
        .studyId(activityLogResource.getStudyId())
        .studyDisplayName(activityLogResource.getStudyDisplayName())
        .cohortId(activityLogResource.getCohortId())
        .cohortDisplayName(activityLogResource.getCohortDisplayName())
        .cohortRevisionId(activityLogResource.getCohortRevisionId())
        .reviewId(activityLogResource.getReviewId())
        .reviewDisplayName(activityLogResource.getReviewDisplayName());
  }
}
