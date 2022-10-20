package bio.terra.tanagra.plugin.accesscontrol;

// TODO: Rename once naming is finalized
public class SetSlashCohort implements IControlledAccessArtifact {
  private final String accessControlType = "set-slash-cohort";

  @Override
  public String getAccessControlType() {
    return accessControlType;
  }

  @Override
  public String getIdentifier() {
    return null;
  }
}
