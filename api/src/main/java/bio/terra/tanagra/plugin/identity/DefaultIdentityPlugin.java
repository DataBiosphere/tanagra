package bio.terra.tanagra.plugin.identity;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.service.jdbc.DataSourceFactory;

public class DefaultIdentityPlugin implements IIdentityPlugin {
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
