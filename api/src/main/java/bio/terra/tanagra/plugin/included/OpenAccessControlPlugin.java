package bio.terra.tanagra.plugin.included;

import java.util.List;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlledEntity;
import bio.terra.tanagra.plugin.identity.User;
import javax.sql.DataSource;

public class OpenAccessControlPlugin implements IAccessControlPlugin {

  @Override
  public void init(PluginConfig config, DataSource dataSource) {
    // do nothing
  }

  @Override
  public boolean checkAccess(User user, IAccessControlledEntity entity) {
    return true;
  }

  @Override
  public boolean grantAccess(User user, IAccessControlledEntity entity) {
    return true;
  }

  @Override
  public boolean revokeAccess(User user, IAccessControlledEntity entity) {
    return true;
  }

  @Override
  public void hydrate(List<? extends IAccessControlledEntity> entities) {
    // do nothing
  }
}
