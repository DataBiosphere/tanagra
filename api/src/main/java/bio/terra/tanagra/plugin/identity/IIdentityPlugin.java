package bio.terra.tanagra.plugin.identity;

import bio.terra.tanagra.plugin.IPlugin;

public interface IIdentityPlugin extends IPlugin {
  User getUserFromIdentifier(String identifier);

  User getCurrentUser();

  Iterable<User> searchUsers(String pattern);
}
