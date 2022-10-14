package bio.terra.tanagra.plugin.accessControl;

public interface IControlledAccessAsset {
    abstract public String getAccessControlType();
    abstract public String getIdentifier();
}
