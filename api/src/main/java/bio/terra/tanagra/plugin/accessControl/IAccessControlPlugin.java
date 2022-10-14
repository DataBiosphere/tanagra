package bio.terra.tanagra.plugin.accessControl;

import bio.terra.tanagra.plugin.IPlugin;
import bio.terra.tanagra.plugin.accessControl.example.User;

public interface IAccessControlPlugin extends IPlugin {
    abstract public boolean checkAccess(User user, IControlledAccessArtifact artifact);
    abstract public boolean grantAccess(User user, IControlledAccessArtifact artifact);
    abstract public boolean revokeAccess(User user, IControlledAccessArtifact artifact);
}
