package bio.terra.tanagra.plugin.included;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlledEntity;
import bio.terra.tanagra.plugin.identity.User;
import java.util.Map;
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
  public void hydrate(Map<String, ? extends IAccessControlledEntity> entities) {
    // do nothing
  }
}
