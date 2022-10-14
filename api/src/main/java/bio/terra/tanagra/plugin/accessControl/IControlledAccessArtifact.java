package bio.terra.tanagra.plugin.accessControl;

public interface IControlledAccessArtifact {
    abstract public String getAccessControlType();
    abstract public String getIdentifier();
}
