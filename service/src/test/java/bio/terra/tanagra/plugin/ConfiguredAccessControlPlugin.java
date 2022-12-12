package bio.terra.tanagra.plugin;

import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.UserId;
import javax.annotation.Nullable;

public class ConfiguredAccessControlPlugin implements AccessControlPlugin {
  @Override
  public void init(PluginConfig config) {
    // do nothing
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, @Nullable ResourceId resourceId) {
    return false;
  }

  @Override
  public ResourceIdCollection listResourceIds(
      UserId userId, ResourceType type, int offset, int limit) {
    return null;
  }
}
