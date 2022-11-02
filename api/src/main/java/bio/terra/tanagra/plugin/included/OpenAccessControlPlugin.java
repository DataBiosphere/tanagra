package bio.terra.tanagra.plugin.included;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlled;
import bio.terra.tanagra.plugin.identity.User;
import javax.sql.DataSource;

public class OpenAccessControlPlugin implements IAccessControlPlugin {

  @Override
  public void init(PluginConfig config, DataSource dataSource) {
    // do nothing
  }

  @Override
  public boolean checkAccess(User user, IAccessControlled artifact) {
    return true;
  }

  @Override
  public boolean grantAccess(User user, IAccessControlled artifact) {
    return true;
  }

  @Override
  public boolean revokeAccess(User user, IAccessControlled artifact) {
    return true;
  }
}
