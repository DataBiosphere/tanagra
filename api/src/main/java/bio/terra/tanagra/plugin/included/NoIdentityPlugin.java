package bio.terra.tanagra.plugin.included;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.plugin.identity.IIdentityPlugin;
import bio.terra.tanagra.plugin.identity.User;
import bio.terra.tanagra.service.jdbc.DataSourceFactory;

public class NoIdentityPlugin implements IIdentityPlugin {
  @Override
  public void init(DataSourceFactory dataSourceFactory, PluginConfig config) {
    // do nothing
  }

  @Override
  public User getUserFromIdentifier(String identifier) {
    return null;
  }

  @Override
  public User getCurrentUser() {
    return null;
  }

  @Override
  public Iterable<User> searchUsers(String pattern) {
    return null;
  }
}
