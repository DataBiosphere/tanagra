package bio.terra.tanagra.plugin.accessControl;

import bio.terra.tanagra.plugin.IPlugin;
import bio.terra.tanagra.plugin.accessControl.example.User;

public interface IAccessControlPlugin extends IPlugin {
    abstract public boolean checkAccess(User user, IControlledAccessAsset entity);
}
