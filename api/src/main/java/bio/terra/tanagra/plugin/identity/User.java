package bio.terra.tanagra.plugin.identity;

public class User {
  private String identifier;

  User(String identifier) {
    this.identifier = identifier;
  }

  public String getIdentifier() {
    return identifier;
  }
}
