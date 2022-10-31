package bio.terra.tanagra.plugin.accesscontrol;

public class Workspace implements IArtifact {
  private final String identifier;
  private String name;

  public Workspace(String identifier) {
    this.identifier = identifier;
  }

  @Override
  public String getArtifactType() {
    return this.getClass().getSimpleName();
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
