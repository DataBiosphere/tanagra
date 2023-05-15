package bio.terra.tanagra.service.accesscontrol.impl;

import bio.terra.tanagra.service.VumcAdminService;
import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.UserId;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.vumc.vda.tanagra.admin.model.ResourceAction;
import org.vumc.vda.tanagra.admin.model.ResourceList;
import org.vumc.vda.tanagra.admin.model.ResourceTypeList;

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
        userId.getSubject(),
        ResourceAction.valueOf(action.toString()),
        org.vumc.vda.tanagra.admin.model.ResourceType.valueOf(resourceType.toString()),
        resourceId == null ? null : resourceId.toString());
  }

  @Override
  public ResourceIdCollection listResourceIds(
      UserId userId, ResourceType resourceType, int offset, int limit) {
    ResourceTypeList resourceTypeList = new ResourceTypeList();
    resourceTypeList.add(
        org.vumc.vda.tanagra.admin.model.ResourceType.valueOf(resourceType.toString()));
    ResourceList resourceList =
        vumcAdminService.listAuthorizedResources(userId.getSubject(), resourceTypeList);
    return ResourceIdCollection.forCollection(
        resourceList.stream()
            .map(resource -> new ResourceId(resource.getId()))
            .collect(Collectors.toList()));
  }
}
