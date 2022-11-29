package bio.terra.tanagra.plugin;

import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.OpenAccessControlPlugin;
import bio.terra.tanagra.service.identity.IdentityPlugin;
import bio.terra.tanagra.service.identity.SingleUserIdentityPlugin;

public enum PluginType {
  ACCESS_CONTROL(AccessControlPlugin.class, OpenAccessControlPlugin.class),
  IDENTITY(IdentityPlugin.class, SingleUserIdentityPlugin.class),
  TEST(TestPlugin.class, TestPluginInternalImplementation.class);

  private final Class<?> type;
  private final Class<?> defaultType;

  <TT extends Plugin, TD extends TT> PluginType(Class<TT> type, Class<TD> defaultType) {
    this.type = type;
    this.defaultType = defaultType;
  }

  public Class<?> getType() {
    return type;
  }

  public Class<?> getDefaultType() {
    return defaultType;
  }
}
