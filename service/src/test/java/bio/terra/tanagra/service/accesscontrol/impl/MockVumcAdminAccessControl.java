package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.authentication.UserId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.vumc.vda.tanagra.admin.model.Resource;
import org.vumc.vda.tanagra.admin.model.ResourceAction;
import org.vumc.vda.tanagra.admin.model.ResourceList;
import org.vumc.vda.tanagra.admin.model.ResourceType;

public class MockVumcAdminAccessControl extends VumcAdminAccessControl {
  private final List<String> admins = new ArrayList<>(); // user emails
  private final Map<String, Set<Resource>> permissions =
      new HashMap<>(); // user email -> set of permissions

  @Override
  protected boolean apiIsAuthorizedUser(String userEmail) {
    return admins.contains(userEmail);
  }

  @Override
  protected ResourceList apiListAuthorizedResources(String userEmail, ResourceType resourceType) {
    ResourceList resourceList = new ResourceList();
    if (permissions.containsKey(userEmail)) {
      permissions.get(userEmail).stream()
          .filter(r -> r.getType().equals(resourceType))
          .forEach(resourceList::add);
    }
    return resourceList;
  }

  public void addAdminUser(UserId user) {
    admins.add(user.getEmail());
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
