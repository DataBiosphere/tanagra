package bio.terra.tanagra.plugin;

import bio.terra.tanagra.service.jdbc.DataSourceFactory;
import bio.terra.tanagra.service.jdbc.DataSourceId;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginService {
  private static final String PLUGIN_DATASOURCE_PARAMETER = "datasource-id";

  private final PluggableConfiguration configuration;
  private final DataSourceFactory dataSourceFactory;
  private final Map<Class<?>, Plugin> availablePlugins = new HashMap<>();

  @Autowired
  public PluginService(PluggableConfiguration configuration, DataSourceFactory dataSourceFactory)
      throws PluginException {
    this.configuration = configuration;
    this.dataSourceFactory = dataSourceFactory;

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
          DataSource dataSource = getDataSource(config.getValue(PLUGIN_DATASOURCE_PARAMETER));

          Class<?> pluginClass = Class.forName(p.getValue().getType());
          Plugin plugin = loadPlugin(pluginClass);

          plugin.init(config, dataSource);

          availablePlugins.put(pluginType.getType(), plugin);
        }
      }
    } catch (ClassNotFoundException e) {
      throw new PluginException(e);
    }
  }

  private DataSource getDataSource(String id) {
    DataSource dataSource = null;

    if (id != null) {
      DataSourceId dataSourceId = new DataSourceId(id);
      dataSource = dataSourceFactory.getDataSource(dataSourceId);
    }

    return dataSource;
  }

  private Plugin loadPlugin(Class<?> pluginClass) throws PluginException {
    try {
      return (Plugin) pluginClass.getConstructor().newInstance();
    } catch (Exception e) {
      throw new PluginException(e);
    }
  }
}
