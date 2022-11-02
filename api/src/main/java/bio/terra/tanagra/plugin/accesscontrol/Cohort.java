package bio.terra.tanagra.plugin.accesscontrol;

public class Cohort implements IAccessControlledEntity {
  private final String identifier;

  Cohort(String identifier) {
    this.identifier = identifier;
  }

  @Override
  public String getAccessControlType() {
    return this.getClass().getSimpleName();
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }
}
