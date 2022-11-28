package bio.terra.tanagra.plugin;

import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.DefaultAccessControlPlugin;
import bio.terra.tanagra.service.identity.DefaultIdentityPlugin;
import bio.terra.tanagra.service.identity.IdentityPlugin;
import java.util.HashMap;
import java.util.Map;

public final class PluginDefaults {
  private static final Map<String, IPlugin> DEFAULTS =
      new HashMap<>(
          Map.of(
              AccessControlPlugin.class.getSimpleName(), new DefaultAccessControlPlugin(),
              IdentityPlugin.class.getSimpleName(), new DefaultIdentityPlugin()));

  private PluginDefaults() {
    // Required for PMD
  }

  public static Map<String, IPlugin> getDefaultPlugins() {
    return DEFAULTS;
  }
}
