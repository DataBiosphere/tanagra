package bio.terra.tanagra.plugin.included;

import bio.terra.tanagra.plugin.PluginConfig;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.IArtifact;
import bio.terra.tanagra.plugin.identity.User;
import bio.terra.tanagra.service.jdbc.DataSourceFactory;

public class OpenAccessControlPlugin implements IAccessControlPlugin {

  @Override
  public void init(DataSourceFactory dataSourceFactory, PluginConfig config) {
    // do nothing
  }

  @Override
  public boolean checkAccess(User user, IArtifact artifact) {
    return true;
  }

  @Override
  public boolean grantAccess(User user, IArtifact artifact) {
    return true;
  }

  @Override
  public boolean revokeAccess(User user, IArtifact artifact) {
    return true;
  }
}
