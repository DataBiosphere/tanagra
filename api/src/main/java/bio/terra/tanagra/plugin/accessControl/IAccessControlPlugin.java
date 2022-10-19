package bio.terra.tanagra.plugin.accessControl;

import bio.terra.tanagra.plugin.IPlugin;
import bio.terra.tanagra.plugin.accessControl.example.User;

public interface IAccessControlPlugin extends IPlugin {
  public abstract boolean checkAccess(User user, IControlledAccessArtifact artifact);

  public abstract boolean grantAccess(User user, IControlledAccessArtifact artifact);

  public abstract boolean revokeAccess(User user, IControlledAccessArtifact artifact);
}
