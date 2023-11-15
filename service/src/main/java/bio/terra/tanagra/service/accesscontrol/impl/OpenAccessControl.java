package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.authentication.UserId;
import javax.annotation.Nullable;

/**
 * Open access control plugin implementation that allows everything: all actions, listing all
 * resources.
 */
public class OpenAccessControl implements AccessControl {
  @Override
  public String getDescription() {
    return "Allows access to any resource by anyone";
  }

  @Override
  public boolean isAuthorized(UserId user, Permissions permissions, @Nullable ResourceId resource) {
    return true;
  }

  @Override
  public ResourceCollection listAllPermissions(
      UserId user, ResourceType type, @Nullable ResourceId parentResource, int offset, int limit) {
    return ResourceCollection.allResourcesAllPermissions(type, parentResource);
  }
}
