package bio.terra.tanagra.plugin;

import bio.terra.tanagra.service.UnderlaysService;
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

  public <T extends Plugin> T getPlugin(String underlayName, Class<T> c) {
    String key = getKey(underlayName, c);
    Plugin plugin = loadedPlugins.get(key);
    if (plugin == null) {
      plugin = loadPlugin(underlayName, c);

      if (plugin == null) {
        throw new PluginException(
            String.format(
                "Plugin '%s' not configured for underlay '%s', and no default is available",
                c.getCanonicalName(), underlayName));
      } else {
        loadedPlugins.put(key, plugin);
      }
    }

    return c.cast(plugin);
  }

  private Plugin loadPlugin(String underlayName, Class<?> c) {
    Plugin plugin;
    Underlay underlay = underlayService.getUnderlay(underlayName);

    Map<String, PluginConfig> pluginConfigs = underlay.getPlugins();
    PluginType pluginType = PluginType.fromType(c);
    if (pluginType == null) {
      throw new PluginException(String.format("'%s' is not a known plugin", c.getCanonicalName()));
    } else {
      try {
        if (pluginConfigs != null && pluginConfigs.containsKey(pluginType.toString())) {
          PluginConfig pluginConfig = pluginConfigs.get(pluginType.toString());

          Class<?> pluginClass = Class.forName(pluginConfig.getType());
          plugin = (Plugin) pluginClass.getConstructor().newInstance();
          plugin.init(pluginConfig);
        } else {
          plugin =
              (Plugin)
                  pluginType.getDefaultImplementationClassName().getConstructor().newInstance();
        }
      } catch (Exception e) {
        throw new PluginException(
            String.format("Unable to load plugin '%s'", pluginType.toString()), e);
      }
    }

    return plugin;
  }

  private String getKey(String underlay, Class<?> pluginClass) {
    return String.format("%s/%s", underlay, pluginClass.getCanonicalName());
  }
}
