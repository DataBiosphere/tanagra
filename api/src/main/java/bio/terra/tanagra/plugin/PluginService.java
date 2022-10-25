package bio.terra.tanagra.plugin;

import static org.springframework.util.StringUtils.capitalize;

import bio.terra.tanagra.plugin.accesscontrol.DefaultAccessControlPlugin;
import bio.terra.tanagra.plugin.accesscontrol.IAccessControlPlugin;
import bio.terra.tanagra.plugin.identity.DefaultIdentityPlugin;
import bio.terra.tanagra.plugin.identity.IIdentityPlugin;
import bio.terra.tanagra.service.jdbc.DataSourceFactory;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginService {
  private final HashMap<String, IPlugin> availablePlugins =
      new HashMap<>() {
        {
          put(IAccessControlPlugin.class.getName(), new DefaultAccessControlPlugin());
          put(IIdentityPlugin.class.getName(), new DefaultIdentityPlugin());
        }
      };

  private final PluggableConfiguration configuration;
  private final DataSourceFactory dataSourceFactory;

  @Autowired
  public PluginService(PluggableConfiguration configuration, DataSourceFactory dataSourceFactory)
      throws PluginException {
    this.configuration = configuration;
    this.dataSourceFactory = dataSourceFactory;

    loadPlugins();
  }

  @SuppressWarnings("unchecked")
  public <T extends IPlugin> T getPlugin(Class c) {
    return (T) availablePlugins.get(c.getName());
  }

  private void loadPlugins() throws PluginException {
    try {
      for (Map.Entry<String, PluginConfig> p : configuration.getPlugins().entrySet()) {
        String key = "I" + capitalize(p.getKey());

        Class<?> pluginClass = Class.forName(p.getValue().getType());
        IPlugin plugin = (IPlugin) pluginClass.getConstructor().newInstance();

        plugin.init(dataSourceFactory, p.getValue());

        availablePlugins.put(key, plugin);
      }
    } catch (Exception e) {
      throw new PluginException(e);
    }
  }
}
