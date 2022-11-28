package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.plugin.PluginConfig;
import javax.annotation.Nullable;
import javax.sql.DataSource;

/**
 * Open access control plugin implementation that allows everything: all actions, listing all
 * resources.
 */
public class OpenAccessControlPlugin implements AccessControlPlugin {
  @Override
  public void init(PluginConfig config, DataSource dataSource) {
    // do nothing
  }

  @Override
  public String getName() {
    return "OPEN_ACCESS_CONTROL_PLUGIN";
  }

  @Override
  public boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, @Nullable ResourceId resourceId) {
    // Every possible action is allowed.
    return true;
  }

  @Override
  public ResourceIdCollection listResourceIds(ResourceType type, int offset, int limit) {
    // Everyone can list everything.
    return ResourceIdCollection.allResourceIds();
  }
}
