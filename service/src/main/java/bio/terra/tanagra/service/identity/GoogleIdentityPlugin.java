package bio.terra.tanagra.service.identity;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.service.accesscontrol.UserId;
import javax.sql.DataSource;

public class GoogleIdentityPlugin implements IdentityPlugin {
  @Override
  public void init(PluginConfig config, DataSource dataSource) {
    // TODO: parse google credential
  }

  @Override
  public UserId getUserId(Object credential) {
    // TODO: parse google credential
    return null;
  }
}
