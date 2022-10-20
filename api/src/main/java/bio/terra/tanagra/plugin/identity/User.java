package bio.terra.tanagra.plugin.identity;

import java.util.List;

public class User {
  private final String identifier;
  private final List<String> roles;

  User(String identifier, List<String> roles) {
    this.identifier = identifier;
    this.roles = roles;
  }

  public String getIdentifier() {
    return identifier;
  }

  public boolean hasRole(String role) {
    return roles.contains(role);
  }
}
