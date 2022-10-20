package bio.terra.tanagra.plugin.accesscontrol;

import bio.terra.tanagra.plugin.IPlugin;
import bio.terra.tanagra.plugin.identity.User;

public interface IAccessControlPlugin extends IPlugin {
  boolean checkAccess(User user, IControlledAccessArtifact artifact);

  boolean grantAccess(User user, IControlledAccessArtifact artifact);

  boolean revokeAccess(User user, IControlledAccessArtifact artifact);
}
