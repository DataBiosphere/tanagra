package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.UserId;
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
  public boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, @Nullable ResourceId resourceId) {
    // Every possible action is allowed.
    return true;
  }

  @Override
  public ResourceIdCollection listResourceIds(
      UserId userId, ResourceType type, int offset, int limit) {
    // Everyone can list everything.
    return ResourceIdCollection.allResourceIds();
  }
}
