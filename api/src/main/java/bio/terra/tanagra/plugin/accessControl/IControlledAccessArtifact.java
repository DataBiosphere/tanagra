package bio.terra.tanagra.plugin.accessControl;

public interface IControlledAccessArtifact {
  public abstract String getAccessControlType();

  public abstract String getIdentifier();
}
