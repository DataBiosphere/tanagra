package bio.terra.tanagra.plugin;

import java.util.HashMap;
import java.util.Map;

import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.underlay.Underlay;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginService {
  private final UnderlaysService underlayService;
  private final Map<String, Plugin> availablePlugins = new HashMap<>();

  @Autowired
  public PluginService(UnderlaysService underlayService) throws PluginException {
    this.underlayService = underlayService;

    loadPlugins();
  }

  public <T extends Plugin> T getPlugin(String underlay, Class<T> c) {
    return c.cast(availablePlugins.get(getKey(underlay, c)));
  }

  private void loadPlugins() throws PluginException {
    for (Underlay underlay : underlayService.getUnderlays()) {
      Map<String, PluginConfig> configuredPlugins = underlay.getPlugins();
      for (PluginType pluginType : PluginType.values()) {
        Plugin plugin;

        try {
          if (configuredPlugins.containsKey(pluginType.toString())) {
            PluginConfig pluginConfig = configuredPlugins.get(pluginType.toString());

            Class<?> pluginClass = Class.forName(pluginConfig.getType());
            plugin = (Plugin) pluginClass.getConstructor().newInstance();
            plugin.init(pluginConfig);
          } else {
            plugin = (Plugin) pluginType.getDefaultType().getConstructor().newInstance();
          }
        } catch (Exception e) {
          throw new PluginException(e);
        }

        availablePlugins.put(getKey(underlay.getName(), pluginType.getType()), plugin);
      }
    }
  }

  private String getKey(String underlay, Class<?> pluginClass) {
    return String.format("%s/%s", underlay, pluginClass.getCanonicalName());
  }
}
