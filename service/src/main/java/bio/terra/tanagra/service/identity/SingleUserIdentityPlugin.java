package bio.terra.tanagra.service.identity;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.service.accesscontrol.UserId;

public class SingleUserIdentityPlugin implements IdentityPlugin {
  @Override
  public void init(PluginConfig config) {
    // do nothing
  }

  @Override
  public UserId getUserId(Object credential) {
    return new UserId("single_user");
  }
}
