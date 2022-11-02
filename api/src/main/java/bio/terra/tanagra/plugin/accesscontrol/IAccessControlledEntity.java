package bio.terra.tanagra.plugin.accesscontrol;

public interface IAccessControlledEntity {
  String getAccessControlType();

  String getIdentifier();
}
