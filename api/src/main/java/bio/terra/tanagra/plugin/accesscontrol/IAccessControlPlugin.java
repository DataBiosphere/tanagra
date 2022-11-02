package bio.terra.tanagra.plugin.accesscontrol;

import bio.terra.tanagra.plugin.IPlugin;
import bio.terra.tanagra.plugin.identity.User;
import java.util.List;

public interface IAccessControlPlugin extends IPlugin {
  boolean checkAccess(User user, IAccessControlledEntity entity);

  boolean grantAccess(User user, IAccessControlledEntity entity);

  boolean revokeAccess(User user, IAccessControlledEntity entity);

  void hydrate(List<? extends IAccessControlledEntity> entities);
}
