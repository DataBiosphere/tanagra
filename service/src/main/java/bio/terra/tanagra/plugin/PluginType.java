package bio.terra.tanagra.plugin;

import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.OpenAccessControlPlugin;
import bio.terra.tanagra.service.identity.IdentityPlugin;
import bio.terra.tanagra.service.identity.SingleUserIdentityPlugin;

public enum PluginType {
  ACCESS_CONTROL(AccessControlPlugin.class, OpenAccessControlPlugin.class),
  IDENTITY(IdentityPlugin.class, SingleUserIdentityPlugin.class);

  private final Class<?> baseClassName;
  private final Class<?> defaultImplementationClassName;

  <TT extends Plugin, TD extends TT> PluginType(
      Class<TT> baseClassName, Class<TD> defaultImplementationClassName) {
    this.baseClassName = baseClassName;
    this.defaultImplementationClassName = defaultImplementationClassName;
  }

  public Class<?> getBaseClassName() {
    return baseClassName;
  }

  public Class<?> getDefaultImplementationClassName() {
    return defaultImplementationClassName;
  }

  public static PluginType fromType(Class<?> type) {
    for (PluginType pluginType : PluginType.values()) {
      if (pluginType.getBaseClassName() == type) {
        return pluginType;
      }
    }

    return null;
  }
}
