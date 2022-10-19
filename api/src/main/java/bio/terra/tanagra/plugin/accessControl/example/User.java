package bio.terra.tanagra.plugin.accessControl.example;

public class User {
  private String identifier;

  User(String identifier) {
    this.identifier = identifier;
  }

  public String getIdentifier() {
    return identifier;
  }
}
