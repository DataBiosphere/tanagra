package bio.terra.tanagra.service.identity;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.service.accesscontrol.UserId;

public class SingleUserIdentityPlugin implements IdentityPlugin {
  @Override
  public void init(PluginConfig config) {
    // do nothing
  }

  @Override
  public String getDescription() {
    return "Single user mode. All events and transactions run under the same identity.";
  }

  @Override
  public UserId getUserId(Object credential) {
    return new UserId("single_user");
  }
}
