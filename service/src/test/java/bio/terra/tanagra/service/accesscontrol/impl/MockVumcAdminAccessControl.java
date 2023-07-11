package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.auth.UserId;
import java.util.*;
import org.vumc.vda.tanagra.admin.model.Resource;
import org.vumc.vda.tanagra.admin.model.ResourceAction;
import org.vumc.vda.tanagra.admin.model.ResourceList;
import org.vumc.vda.tanagra.admin.model.ResourceType;

public class MockVumcAdminAccessControl extends VumcAdminAccessControl {
  private final Map<String, Set<Resource>> permissions =
      new HashMap<>(); // user email -> set of permissions

  @Override
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  protected boolean apiIsAuthorized(
      String userEmail,
      ResourceAction resourceAction,
      ResourceType resourceType,
      String resourceId) {
    return permissions.containsKey(userEmail)
        && (permissions
                .get(userEmail)
                .contains(new Resource().action(resourceAction).type(resourceType).id(resourceId))
            || permissions
                .get(userEmail)
                .contains(
                    new Resource().action(ResourceAction.ALL).type(resourceType).id(resourceId)));
  }

  @Override
  protected ResourceList apiListAuthorizedResources(String userEmail, ResourceType resourceType) {
    ResourceList resourceList = new ResourceList();
    if (permissions.containsKey(userEmail)) {
      permissions.get(userEmail).stream()
          .filter(r -> r.getType().equals(resourceType))
          .forEach(r -> resourceList.add(r));
    }
    return resourceList;
  }

  public void addPermission(
      UserId user, ResourceAction action, ResourceType type, String resourceId) {
    Set<Resource> permissionsForUser =
        permissions.containsKey(user.getEmail())
            ? permissions.get(user.getEmail())
            : new HashSet<>();
    permissionsForUser.add(new Resource().action(action).type(type).id(resourceId));
    permissions.put(user.getEmail(), permissionsForUser);
  }
}
