package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.VumcAdminService;
import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.UserId;
import bio.terra.tanagra.vumc.admin.model.ResourceIdList;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class VumcAdminAccessControl implements AccessControl {
  private final VumcAdminService vumcAdminService;

  public VumcAdminAccessControl(VumcAdminService vumcAdminService) {
    this.vumcAdminService = vumcAdminService;
  }

  @Override
  public String getDescription() {
    return "Forward the access control checks to the VUMC admin service";
  }

  @Override
  public boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, @Nullable ResourceId resourceId) {
    return vumcAdminService.isAuthorized(
        action.toString(),
        resourceType.toString(),
        resourceId == null ? null : resourceId.toString(),
        userId.getEmail());
  }

  @Override
  public ResourceIdCollection listResourceIds(
      UserId userId, ResourceType resourceType, int offset, int limit) {
    ResourceIdList vumcResourceIdList =
        vumcAdminService.listAuthorizedResources(resourceType.toString(), userId.getEmail());
    return ResourceIdCollection.forCollection(
        vumcResourceIdList.stream()
            .map(resourceIdStr -> new ResourceId(resourceIdStr))
            .collect(Collectors.toList()));
  }
}
