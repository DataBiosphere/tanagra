package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.auth.UserId;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class MockAouWorkbenchAccessControl extends AouWorkbenchAccessControl {
  // Map of user email -> map of (workspace id -> role).
  private final Map<String, Map<String, WorkspaceRole>> permissions = new HashMap<>();

  @Override
  protected @Nullable WorkspaceRole apiGetWorkspaceAccess(UserId userId, String workspaceId) {
    return permissions.containsKey(userId.getEmail())
        ? permissions.get(userId.getEmail()).get(workspaceId)
        : null;
  }

  public void addPermission(UserId user, String workspaceId, WorkspaceRole role) {
    Map<String, WorkspaceRole> permissionsForUser =
        permissions.containsKey(user.getEmail())
            ? permissions.get(user.getEmail())
            : new HashMap<>();
    permissionsForUser.put(workspaceId, role);
    permissions.put(user.getEmail(), permissionsForUser);
  }
}
