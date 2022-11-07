package bio.terra.tanagra.plugin.included;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.plugin.identity.IIdentityPlugin;
import bio.terra.tanagra.plugin.identity.User;
import java.util.List;
import javax.sql.DataSource;

public class NoIdentityPlugin implements IIdentityPlugin {
  @Override
  public void init(PluginConfig config, DataSource dataSource) {
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

  @Override
  public void hydrate(List<User> users) {
    users.forEach(
        user -> {
          user.setGivenName("unknown givenname");
          user.setSurname("unknown surname");
        });
  }
}
