package bio.terra.tanagra.plugin.accesscontrol;

public class Cohort implements IArtifact {
  private final String identifier;

  Cohort(String identifier) {
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
}
