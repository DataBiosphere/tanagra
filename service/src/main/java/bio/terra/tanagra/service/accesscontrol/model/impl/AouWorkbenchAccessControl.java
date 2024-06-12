package bio.terra.tanagra.service.accesscontrol.model.impl;

import bio.terra.tanagra.service.accesscontrol.AccessControlHelper;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol.model.StudyAccessControl;
import bio.terra.tanagra.service.authentication.UserId;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class AouWorkbenchAccessControl implements StudyAccessControl {
  private static final Logger LOGGER = LoggerFactory.getLogger(AouWorkbenchAccessControl.class);
  private String basePath;

  @Override
  public String getDescription() {
    return "AoU-RW study-based access control";
  }

  @Override
  public void initialize(
      List<String> params,
      String basePath,
      String oauthClientId,
      AccessControlHelper accessControlHelper) {
    // Store the basePath, so we can use it when calling the workbench API.
    // oauthClientId not used for AoU RW call
    if (basePath == null) {
      throw new IllegalArgumentException("Base URL is required for Workbench API calls");
    }
    this.basePath = basePath;
  }

  @Override
  public Permissions getUnderlay(UserId user, ResourceId underlay) {
    // TODO: Call workbench to check for RT/CT access. See "/v1/profile" endpoint to get user
    // profile.
    return Permissions.allActions(ResourceType.UNDERLAY);
  }

  @Override
  public ResourceCollection listUnderlays(UserId user, int offset, int limit) {
    LOGGER.error("AoU will not call Tanagra to list underlays - this is managed by the Workbench.");
    return ResourceCollection.empty(ResourceType.UNDERLAY, null);
  }

  @Override
  public Permissions createStudy(UserId user) {
    // Workbench creates a study as part of workspace creation. Always allow.
    return Permissions.forActions(ResourceType.STUDY, Set.of(Action.CREATE));
  }

  @Override
  public ResourceCollection listStudies(UserId user, int offset, int limit) {
    LOGGER.error("AoU will not call Tanagra to list studies - this is managed by the Workbench.");
    return ResourceCollection.empty(ResourceType.STUDY, null);
  }

  @Override
  public Permissions getStudy(UserId user, ResourceId study) {
    // Call the workbench to get the user's role on the workspace that contains this study.
    // Expect the Tanagra study id to match the Workbench workspaceNamespace.
    AouWorkbenchAccessControl.WorkspaceRole role = apiGetWorkspaceAccess(user, study.getStudy());
    return role == null
        ? Permissions.empty(ResourceType.STUDY)
        : Permissions.forActions(ResourceType.STUDY, role.getStudyPermissions());
  }

  public enum WorkspaceRole {
    OWNER(ResourceType.STUDY.getActions()),
    WRITER(Set.of(Action.READ, Action.UPDATE, Action.CREATE_COHORT, Action.CREATE_CONCEPT_SET)),
    READER(Set.of(Action.READ));

    private final Set<Action> studyPermissions;

    WorkspaceRole(Set<Action> studyPermissions) {
      this.studyPermissions = studyPermissions;
    }

    public Set<Action> getStudyPermissions() {
      return studyPermissions;
    }
  }

  /**
   * @return the role that the user has on the workspace, or null if the user has no permissions.
   */
  protected @Nullable AouWorkbenchAccessControl.WorkspaceRole apiGetWorkspaceAccess(
      UserId userId, String workspaceNamespace) {
    LOGGER.debug(
        "AoU Workspace Access Level api check access to workspace {} for user {}",
        workspaceNamespace,
        userId.getEmail());
    String url = basePath + "/v1/workspaces/access/" + workspaceNamespace;
    LOGGER.debug("AoU Workspace Access Level api endpoint : {}}", url);

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add("Content-Type", "application/json");
    httpHeaders.add("Accept", "application/json");
    httpHeaders.add("Authorization", "Bearer " + userId.getToken());

    HttpEntity<String> requestEntity = new HttpEntity<>("", httpHeaders);
    ResponseEntity<String> responseEntity =
        restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
    if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
      String accessLevel = responseEntity.getBody();
      LOGGER.info(
          "AoU Workspace Access Level api User {} has {} access level for workspace {}",
          userId.getEmail(),
          accessLevel,
          workspaceNamespace);
      try {
        // if NO ACCESS throws IllegalArgumentException or null throws NPE
        return AouWorkbenchAccessControl.WorkspaceRole.valueOf(accessLevel);
      } catch (Exception ex) {
        LOGGER.error(
            "AoU Workspace Access Level api no WorkspaceRole Enum defined for access level {}.",
            accessLevel,
            ex);
        return null;
      }
    }

    HttpStatus statusCode = (HttpStatus) responseEntity.getStatusCode();
    LOGGER.error(
        "AoU Workspace Access Level api error. Http status code: {}, Message: {}",
        statusCode,
        statusCode.getReasonPhrase());
    return null;
  }
}
