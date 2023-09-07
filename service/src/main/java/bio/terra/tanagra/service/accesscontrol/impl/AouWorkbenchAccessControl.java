package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.accesscontrol.*;
import bio.terra.tanagra.service.auth.UserId;
import java.util.*;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * AoU Researcher Workbench access control plugin implementation that checks permissions on the
 * workspace that is mapped to each study.
 */
public class AouWorkbenchAccessControl implements AccessControl {
  private static final Logger LOGGER = LoggerFactory.getLogger(AouWorkbenchAccessControl.class);
  // Workspace owners have all permissions in a study.
  private static final Map<ResourceType, Set<Action>> WORKSPACE_OWNER_PERMISSIONS =
      Map.of(
          ResourceType.STUDY,
          Set.of(
              Action.READ,
              Action.UPDATE,
              Action.DELETE,
              Action.CREATE_COHORT,
              Action.CREATE_CONCEPT_SET),
          ResourceType.COHORT,
          ResourceType.COHORT.getActions(), // All cohort actions.
          ResourceType.CONCEPT_SET,
          ResourceType.CONCEPT_SET.getActions(), // All concept set actions.
          ResourceType.REVIEW,
          ResourceType.REVIEW.getActions(), // All review actions.
          ResourceType.ANNOTATION_KEY,
          ResourceType.ANNOTATION_KEY.getActions()); // All annotation key actions.

  // Workspace writers have all permissions for a study, except delete and update actions.
  private static final Map<ResourceType, Set<Action>> WORKSPACE_WRITER_PERMISSIONS =
      Map.of(
          ResourceType.STUDY,
          Set.of(Action.READ, Action.CREATE_COHORT, Action.CREATE_CONCEPT_SET),
          ResourceType.COHORT,
          ResourceType.COHORT.getActions(), // All cohort actions.
          ResourceType.CONCEPT_SET,
          ResourceType.CONCEPT_SET.getActions(), // All concept set actions.
          ResourceType.REVIEW,
          ResourceType.REVIEW.getActions(), // All review actions.
          ResourceType.ANNOTATION_KEY,
          ResourceType.ANNOTATION_KEY.getActions()); // All annotation key actions.

  // Workspace readers cannot persist any changes to a study, only view the existing state.
  private static final Map<ResourceType, Set<Action>> WORKSPACE_READER_PERMISSIONS =
      Map.of(
          ResourceType.STUDY, Set.of(Action.READ),
          ResourceType.COHORT, Set.of(Action.READ),
          ResourceType.CONCEPT_SET, Set.of(Action.READ),
          ResourceType.REVIEW, Set.of(Action.READ, Action.QUERY_INSTANCES, Action.QUERY_COUNTS),
          ResourceType.ANNOTATION_KEY, Set.of(Action.READ));

  private String basePath;

  @Override
  public String getDescription() {
    return "Check AoU Researcher Workbench for workspace access";
  }

  @Override
  public void initialize(List<String> params, String basePath, String oauthClientId) {
    // Store the basePath, so we can use it when calling the workbench API.
    // oauthClientId not used for AoU RW call
    if (basePath == null) {
      throw new IllegalArgumentException("Base URL is required for Workbench API calls");
    }
    this.basePath = basePath;
  }

  @Override
  public boolean isAuthorized(UserId user, Permissions permissions, @Nullable ResourceId resource) {
    if (permissions.getType().equals(ResourceType.UNDERLAY)) {
      // Browsing the cohort builder without saving. Always allow.
      // For AoU underlays - browsing is not allowed, but return true for tanagra internal
      // consider using "/v1/profile" endpoint to get user profile for checking RT/CT access
      return true;
    } else if (Permissions.forActions(ResourceType.STUDY, Action.CREATE).equals(permissions)) {
      // Workbench creates a Tanagra study as part of workspace creation. Always allow.
      return true;
    } else {
      // Check for some permissions on a particular artifact (e.g. study, cohort).
      if (resource == null || resource.getStudy() == null) {
        LOGGER.error(
            "AoU Workspace Access Level api Study id is required to check authorization for resource {}",
            resource);
        return false;
      }

      // Call the workbench to get the user's role on the workspace that contains this study.
      // Expect the Tanagra study id to match the Workbench workspaceNamespace.
      WorkspaceRole role = apiGetWorkspaceAccess(user, resource.getStudy());
      if (role == null) {
        return false;
      }

      // Map the workspace role to Tanagra permissions.
      return role.getTanagraPermissions()
          .get(permissions.getType())
          .containsAll(permissions.getActions());
    }
  }

  @Override
  public ResourceCollection listAllPermissions(
      UserId user, ResourceType type, @Nullable ResourceId parentResource, int offset, int limit) {
    if (ResourceType.UNDERLAY.equals(type) || ResourceType.STUDY.equals(type)) {
      // AoU will not call to list Underlays or list studies - this is managed by Workbench
      LOGGER.error("Calls from AoU Workbench should never get here. ");
      return ResourceCollection.empty(type, null);
    } else {
      // Study is parent of cohorts, annotations, concept_sets and datasets
      // parentResource.getStudy is required to check permissions on these artifacts
      if (parentResource == null || parentResource.getStudy() == null) {
        LOGGER.error(
            "Study id is required to list permissions for a child artifact: {}", parentResource);
        return ResourceCollection.empty(type, parentResource);
      }

      // Call the workbench to get the user's role on the workspace that contains this study.
      // Expect the tanagra study id to match the Workbench workspace id.
      WorkspaceRole role = apiGetWorkspaceAccess(user, parentResource.getStudy());
      if (role == null) {
        return ResourceCollection.empty(type, parentResource);
      }

      // Map the workspace role to Tanagra permissions.
      return ResourceCollection.allResourcesSamePermissions(
          Permissions.forActions(type, role.getTanagraPermissions().get(type)), parentResource);
    }
  }

  public enum WorkspaceRole {
    OWNER(WORKSPACE_OWNER_PERMISSIONS),
    WRITER(WORKSPACE_WRITER_PERMISSIONS),
    READER(WORKSPACE_READER_PERMISSIONS);

    private final Map<ResourceType, Set<Action>> tanagraPermissions;

    WorkspaceRole(Map<ResourceType, Set<Action>> tanagraPermissions) {
      this.tanagraPermissions = tanagraPermissions;
    }

    public Map<ResourceType, Set<Action>> getTanagraPermissions() {
      return Collections.unmodifiableMap(tanagraPermissions);
    }
  }

  /**
   * @return the role that the user has on the workspace, or null if the user has no permissions.
   */
  protected @Nullable WorkspaceRole apiGetWorkspaceAccess(
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
      String accesslevel = responseEntity.getBody();
      LOGGER.info(
          "AoU Workspace Access Level api User {} has {} access level for workspace {}",
          userId.getEmail(),
          accesslevel,
          workspaceNamespace);
      try {
        // if NO ACCESS throws IllegalArgumentException or null throws NPE
        return WorkspaceRole.valueOf(accesslevel);
      } catch (Exception ex) {
        LOGGER.error(
            "AoU Workspace Access Level api no WorkspaceRole Enum defined for access level {}.",
            accesslevel,
            ex);
        return null;
      }
    }
    LOGGER.error(
        "AoU Workspace Access Level api error. Http status code: {}, Message: {}",
        responseEntity.getStatusCodeValue(),
        responseEntity.getStatusCode().getReasonPhrase());
    return null;
  }
}
