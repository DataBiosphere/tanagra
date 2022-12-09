package bio.terra.tanagra.plugin;

import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.OpenAccessControlPlugin;

public enum PluginType {
  ACCESS_CONTROL(AccessControlPlugin.class, OpenAccessControlPlugin.class);

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
}
