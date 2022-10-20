package bio.terra.tanagra.plugin.accesscontrol;

import bio.terra.tanagra.plugin.IPlugin;
import bio.terra.tanagra.plugin.identity.User;

public interface IAccessControlPlugin extends IPlugin {
  boolean checkAccess(User user, IArtifact artifact);

  boolean grantAccess(User user, IArtifact artifact);

  boolean revokeAccess(User user, IArtifact artifact);
}
