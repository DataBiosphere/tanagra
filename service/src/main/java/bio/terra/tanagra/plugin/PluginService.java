package bio.terra.tanagra.plugin;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginService {
  private final PluggableConfiguration configuration;
  private final Map<Class<?>, Plugin> availablePlugins = new HashMap<>();

  @Autowired
  public PluginService(PluggableConfiguration configuration) throws PluginException {
    this.configuration = configuration;

    loadDefaultPlugins();
    loadPlugins();
  }

  public <T extends Plugin> T getPlugin(Class<T> c) {
    return c.cast(availablePlugins.get(c));
  }

  private void loadDefaultPlugins() throws PluginException {
    for (PluginType pluginType : PluginType.values()) {
      availablePlugins.put(pluginType.getType(), loadPlugin(pluginType.getDefaultType()));
    }
  }

  private void loadPlugins() throws PluginException {
    try {
      if (configuration.isConfigured()) {
        for (Map.Entry<String, PluginConfig> p : configuration.getPlugins().entrySet()) {
          PluginType pluginType = PluginType.valueOf(p.getKey());
          PluginConfig config = p.getValue();

          Class<?> pluginClass = Class.forName(p.getValue().getType());
          Plugin plugin = loadPlugin(pluginClass);

          plugin.init(config);

          availablePlugins.put(pluginType.getType(), plugin);
        }
      }
    } catch (ClassNotFoundException e) {
      throw new PluginException(e);
    }
  }

  private Plugin loadPlugin(Class<?> pluginClass) throws PluginException {
    try {
      return (Plugin) pluginClass.getConstructor().newInstance();
    } catch (Exception e) {
      throw new PluginException(e);
    }
  }
}
