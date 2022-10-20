package bio.terra.tanagra.plugin.accesscontrol;

// TODO: Rename once naming is finalized
public class StudySlashWorkspace implements IControlledAccessArtifact {
  private final String accessControlType = "study-slash-workspace";

  @Override
  public String getAccessControlType() {
    return accessControlType;
  }

  @Override
  public String getIdentifier() {
    return null;
  }
}
