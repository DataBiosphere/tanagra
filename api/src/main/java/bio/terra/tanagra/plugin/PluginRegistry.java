package bio.terra.tanagra.plugin;

import java.util.HashMap;

import bio.terra.tanagra.plugin.accessControl.DefaultAccessControlPlugin;
import bio.terra.tanagra.plugin.accessControl.IAccessControlPlugin;

public class PluginRegistry {
    private static HashMap<String, IPlugin> availablePlugins;

    public static void Discover() {
        // TODO: Discover available plugins

        availablePlugins = new HashMap<String, IPlugin>() {{
            put(IAccessControlPlugin.class.getName(), new DefaultAccessControlPlugin());
        }};

        availablePlugins.get(IAccessControlPlugin.class.getName()).init(new PluginConfig());
    }

    public static <T extends IPlugin> T getPlugin(Class c) {
        T t = (T) availablePlugins.get(c.getName());

        return t;
    }
}
