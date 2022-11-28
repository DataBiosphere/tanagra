package bio.terra.tanagra.service.identity;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.service.accesscontrol.UserId;
import javax.sql.DataSource;

public class SingleUserIdentityPlugin implements IdentityPlugin {
  @Override
  public void init(PluginConfig config, DataSource dataSource) {
    // do nothing
  }

  @Override
  public UserId getUserId(Object credential) {
    return new UserId("single_user");
  }
}
