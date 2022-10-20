package bio.terra.tanagra.plugin;

import bio.terra.tanagra.plugin.accesscontrol.DefaultAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.identity.DefaultIdentityPlugin;
import bio.terra.tanagra.plugin.identity.IIdentityPlugin;
import java.util.HashMap;

public class PluginRegistry {
  private static HashMap<String, IPlugin> availablePlugins;

  private PluginRegistry() {
    // not called, required to pass checkstyle
  }

  public static void discover() {
    // TODO: Discover available plugins

    availablePlugins =
        new HashMap<String, IPlugin>() {
          {
            put(IAccessControlPlugin.class.getName(), new DefaultAccessControlPlugin());
            put(IIdentityPlugin.class.getName(), new DefaultIdentityPlugin());
          }
        };

    availablePlugins.get(IAccessControlPlugin.class.getName()).init(new PluginConfig());
    availablePlugins.get(IIdentityPlugin.class.getName()).init(new PluginConfig());
  }

  @SuppressWarnings("unchecked")
  public static <T extends IPlugin> T getPlugin(Class c) {
    T t = (T) availablePlugins.get(c.getName());

    return t;
  }
}
