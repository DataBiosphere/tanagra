package bio.terra.tanagra.plugin.identity;

import bio.terra.tanagra.plugin.PluginConfig;

public class DefaultIdentityPlugin implements IIdentityPlugin {
  private PluginConfig config;

  @Override
  public void init(PluginConfig config) {
    this.config = config;
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
