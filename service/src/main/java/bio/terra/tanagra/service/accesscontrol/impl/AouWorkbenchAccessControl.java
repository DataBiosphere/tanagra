package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.accesscontrol.*;
import bio.terra.tanagra.service.auth.UserId;
import java.util.*;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  // Workspace writers have all permissions in a study, except for deleting the study.
  private static final Map<ResourceType, Set<Action>> WORKSPACE_WRITER_PERMISSIONS =
      Map.of(
          ResourceType.STUDY,
          Set.of(Action.READ, Action.UPDATE, Action.CREATE_COHORT, Action.CREATE_CONCEPT_SET),
          ResourceType.COHORT,
          ResourceType.COHORT.getActions(), // All cohort actions.
          ResourceType.CONCEPT_SET,
          ResourceType.CONCEPT_SET.getActions(), // All concept set actions.
          ResourceType.REVIEW,
          ResourceType.REVIEW.getActions(), // All review actions.
          ResourceType.ANNOTATION_KEY,
          ResourceType.ANNOTATION_KEY.getActions()); // All annotation key actions.

  // Workspace readers cannnot persist any changes to a study, only view the existing state.
  private static final Map<ResourceType, Set<Action>> WORKSPACE_READER_PERMISSIONS =
      Map.of(
          ResourceType.STUDY, Set.of(Action.READ),
          ResourceType.COHORT, Set.of(Action.READ),
          ResourceType.CONCEPT_SET, Set.of(Action.READ),
          ResourceType.REVIEW, Set.of(Action.READ, Action.QUERY_INSTANCES, Action.QUERY_COUNTS),
          ResourceType.ANNOTATION_KEY, Set.of(Action.READ));
  private String basePath;
  private String oauthClientId;

  @Override
  public String getDescription() {
    return "Check AoU Researcher Workbench for workspace access";
  }

  @Override
  public void initialize(List<String> params, String basePath, String oauthClientId) {
    // Store the basePath and oauthClientId first, so we can use it when calling the workbench API.
    if (basePath == null || oauthClientId == null) {
      throw new IllegalArgumentException(
          "Base URL and OAuth client id are required for Workbench API calls");
    }
    this.basePath = basePath;
    this.oauthClientId = oauthClientId;
  }

  @Override
  public boolean isAuthorized(UserId user, Permissions permissions, @Nullable ResourceId resource) {
    if (permissions.getType().equals(ResourceType.UNDERLAY)) {
      // Browsing the cohort builder without saving. Always allow.
      return true;
    } else if (Permissions.forActions(ResourceType.STUDY, Action.CREATE).equals(permissions)) {
      // Workbench creates a Tanagra study as part of workspace creation. Always allow.
      return true;
    } else {
      // Check for some permissions on a particular artifact (e.g. study, cohort).
      if (resource == null || resource.getStudy() == null) {
        LOGGER.error(
            "Study id is required to check authorization of a particular artifact: {}", resource);
        return false;
      }

      // Call the workbench to get the user's role on the workspace that contains this study.
      // Expect the Tanagra study id to match the Workbench workspace id.
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
    if (ResourceType.UNDERLAY.equals(type)) {
      // Browsing the cohort builder without saving. Allow for all underlays.
      return ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null);
    } else if (ResourceType.STUDY.equals(type)) {
      // Call the workbench to get the list of workspaces the user can see, and their role on each.
      Map<String, WorkspaceRole> workspaceRoleMap = apiListWorkspacesWithAccess(user);

      // Map the workspace roles to Tanagra permissions.
      Map<ResourceId, Permissions> studyPermissionsMap = new HashMap<>();
      workspaceRoleMap.entrySet().stream()
          .forEach(
              entry -> {
                String studyId = entry.getKey();
                WorkspaceRole role = entry.getValue();
                studyPermissionsMap.put(
                    ResourceId.forStudy(studyId),
                    Permissions.forActions(
                        ResourceType.STUDY, role.getTanagraPermissions().get(ResourceType.STUDY)));
              });
      return studyPermissionsMap.isEmpty()
          ? ResourceCollection.empty(type, parentResource)
          : ResourceCollection.resourcesDifferentPermissions(studyPermissionsMap);
    } else {
      // Check for some permissions on a child artifact of a study (e.g. cohort).
      if (parentResource == null || parentResource.getStudy() == null) {
        LOGGER.error(
            "Study id is required to list permissions for a child artifact: {}", parentResource);
        return ResourceCollection.empty(type, parentResource);
      }

      // Call the workbench to get the user's role on the workspace that contains this study.
      // Expect the Tanagra study id to match the Workbench workspace id.
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
  protected @Nullable WorkspaceRole apiGetWorkspaceAccess(UserId userId, String workspaceId) {
    LOGGER.debug(
        "Calling workbench API to check access to workspace {} for user {}",
        workspaceId,
        userId.getEmail());
    LOGGER.debug("Workbench API base path: {}, oauth client id: {}", basePath, oauthClientId);
    // TODO: Call the workbench API to get the role the user has on the given workspace.
    return WorkspaceRole.OWNER; // Temporarily, everyone is an owner on every workspace.
  }

  /** @return a map of the workspace id -> role that the user has on each. */
  protected Map<String, WorkspaceRole> apiListWorkspacesWithAccess(UserId userId) {
    LOGGER.debug(
        "Calling workbench API to list workspaces user {} can see, and their role on each.",
        userId.getEmail());
    LOGGER.debug("Workbench API base path: {}, oauth client id: {}", basePath, oauthClientId);
    // TODO: Call the workbench API to list the workspaces the user has access to, and their role on
    // each.
    return Map.of(); // Temporarily, no one has access to any worksapces.
  }
}
