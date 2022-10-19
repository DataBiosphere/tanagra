package bio.terra.tanagra.plugin.identity;

import bio.terra.tanagra.plugin.IPlugin;

public interface IIdentityPlugin extends IPlugin {
  public abstract User getUserFromIdentifier(String identifier);

  public abstract User getCurrentUser();

  public abstract Iterable<User> searchUsers(String pattern);
}
