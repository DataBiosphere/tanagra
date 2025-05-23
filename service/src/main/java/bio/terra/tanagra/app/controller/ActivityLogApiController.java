package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.ACTIVITY_LOG;

import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.configuration.VersionConfiguration;
import bio.terra.tanagra.generated.controller.ActivityLogApi;
import bio.terra.tanagra.generated.model.ApiActivityLogEntry;
import bio.terra.tanagra.generated.model.ApiActivityLogEntryAdditionalInfo;
import bio.terra.tanagra.generated.model.ApiActivityLogEntryList;
import bio.terra.tanagra.generated.model.ApiActivityType;
import bio.terra.tanagra.generated.model.ApiProperties;
import bio.terra.tanagra.generated.model.ApiPropertyKeyValue;
import bio.terra.tanagra.generated.model.ApiResourceObject;
import bio.terra.tanagra.generated.model.ApiResourceType;
import bio.terra.tanagra.generated.model.ApiSystemVersion;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.artifact.ActivityLogService;
import bio.terra.tanagra.service.artifact.model.ActivityLog;
import bio.terra.tanagra.service.artifact.model.ActivityLogResource;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ActivityLogApiController implements ActivityLogApi {
  private final ActivityLogService activityLogService;
  private final AccessControlService accessControlService;

  @Autowired
  public ActivityLogApiController(
      ActivityLogService activityLogService, AccessControlService accessControlService) {
    this.activityLogService = activityLogService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiActivityLogEntryList> listActivityLogEntries(
      String userEmail,
      Boolean exactMatch,
      ApiResourceType resourceType,
      ApiActivityType activityType,
      Integer offset,
      Integer limit) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), Permissions.forActions(ACTIVITY_LOG, READ));
    ApiActivityLogEntryList apiActivityLogs = new ApiActivityLogEntryList();
    activityLogService
        .listActivityLogs(
            offset,
            limit,
            userEmail,
            exactMatch,
            activityType == null ? null : ActivityLog.Type.valueOf(activityType.name()),
            resourceType == null ? null : ActivityLogResource.Type.valueOf(resourceType.name()))
        .forEach(activityLog -> apiActivityLogs.add(toApiObject(activityLog)));
    return ResponseEntity.ok(apiActivityLogs);
  }

  @Override
  public ResponseEntity<ApiActivityLogEntry> getActivityLogEntry(String activityLogEntryId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), Permissions.forActions(ACTIVITY_LOG, READ));
    return ResponseEntity.ok(toApiObject(activityLogService.getActivityLog(activityLogEntryId)));
  }

  private ApiActivityLogEntry toApiObject(ActivityLog activityLog) {
    return new ApiActivityLogEntry()
        .id(activityLog.getId())
        .userEmail(activityLog.getUserEmail())
        .logged(activityLog.getLogged())
        .systemVersion(
            new ApiSystemVersion()
                .gitTag(activityLog.getVersionGitTag())
                .gitHash(activityLog.getVersionGitHash())
                .github(VersionConfiguration.getGithubUrl(activityLog.getVersionGitHash()))
                .build(activityLog.getVersionBuild()))
        .activityType(ApiActivityType.valueOf(activityLog.getType().name()))
        .resources(
            activityLog.getResources().stream().map(this::toApiObject).collect(Collectors.toList()))
        .additionalInfo(
            new ApiActivityLogEntryAdditionalInfo()
                .exportModel(activityLog.getExportModel())
                .recordsCount(activityLog.getRecordsCount()));
  }

  private ApiResourceObject toApiObject(ActivityLogResource activityLogResource) {
    ApiResourceObject apiResource =
        new ApiResourceObject()
            .type(ApiResourceType.valueOf(activityLogResource.getType().name()))
            .studyId(activityLogResource.getStudyId())
            .studyDisplayName(activityLogResource.getStudyDisplayName())
            .cohortId(activityLogResource.getCohortId())
            .cohortDisplayName(activityLogResource.getCohortDisplayName())
            .cohortRevisionId(activityLogResource.getCohortRevisionId())
            .reviewId(activityLogResource.getReviewId())
            .reviewDisplayName(activityLogResource.getReviewDisplayName());
    if (activityLogResource.getStudyProperties() != null) {
      ApiProperties apiProperties = new ApiProperties();
      activityLogResource
          .getStudyProperties()
          .forEach(
              (key, value) -> apiProperties.add(new ApiPropertyKeyValue().key(key).value(value)));
      apiResource.studyProperties(apiProperties);
    }
    return apiResource;
  }
}
