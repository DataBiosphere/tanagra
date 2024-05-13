package bio.terra.tanagra.service.accesscontrol2.impl;

import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol2.UnderlayAccessControl;
import bio.terra.tanagra.service.authentication.UserId;

public class OpenAccessControl implements UnderlayAccessControl {
  @Override
  public String getDescription() {
    return "Open access control -- no authorization checks";
  }

  @Override
  public ResourceCollection listUnderlays(UserId user, int offset, int limit) {
    return ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null);
  }

  @Override
  public Permissions getUnderlay(UserId user, ResourceId underlay) {
    return Permissions.allActions(ResourceType.UNDERLAY);
  }
}
