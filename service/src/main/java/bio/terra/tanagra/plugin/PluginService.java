package bio.terra.tanagra.plugin;

import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.identity.IdentityPlugin;
import bio.terra.tanagra.underlay.Underlay;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginService {
  private final UnderlaysService underlayService;
  private final Map<String, Plugin> loadedPlugins = new HashMap<>();

  @Autowired
  public PluginService(UnderlaysService underlayService) {
    this.underlayService = underlayService;
  }

  public AccessControlPlugin getAccessControlPlugin(String underlayName) {
    return (AccessControlPlugin) getPlugin(underlayName, PluginType.ACCESS_CONTROL);
  }

  public IdentityPlugin getIdentityPlugin(String underlayName) {
    return (IdentityPlugin) getPlugin(underlayName, PluginType.IDENTITY);
  }

  private Plugin getPlugin(String underlayName, PluginType type) {
    String key = getKey(underlayName, type);
    Plugin plugin = loadedPlugins.get(key);
    if (plugin == null) {
      plugin = loadPlugin(underlayName, type);
      loadedPlugins.put(key, plugin);
    }

    return plugin;
  }

  private Plugin loadPlugin(String underlayName, PluginType pluginType) {
    Plugin plugin;
    Underlay underlay = underlayService.getUnderlay(underlayName);

    Map<String, PluginConfig> pluginConfigs = underlay.getPluginConfigs();

    try {
      if (pluginConfigs.containsKey(pluginType.toString())) {
        PluginConfig pluginConfig = pluginConfigs.get(pluginType.toString());

        Class<?> pluginClass = Class.forName(pluginConfig.getImplementationClassName());
        plugin = (Plugin) pluginClass.getConstructor().newInstance();
        plugin.init(pluginConfig);
      } else {
        plugin =
            (Plugin) pluginType.getDefaultImplementationClassName().getConstructor().newInstance();
      }
    } catch (Exception e) {
      throw new PluginException(
          String.format("Unable to load plugin '%s'", pluginType.toString()), e);
    }

    return plugin;
  }

  private String getKey(String underlayName, PluginType pluginType) {
    return String.format("%s/%s", underlayName, pluginType.toString());
  }
}
