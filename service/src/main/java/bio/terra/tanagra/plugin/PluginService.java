package bio.terra.tanagra.plugin;

import static org.springframework.util.StringUtils.capitalize;

import bio.terra.tanagra.service.jdbc.DataSourceFactory;
import bio.terra.tanagra.service.jdbc.DataSourceId;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginService {
  private static final String PLUGIN_DATASOURCE_PARAMETER = "datasource-id";

  private final PluggableConfiguration configuration;
  private final DataSourceFactory dataSourceFactory;
  private final Map<String, Plugin> availablePlugins;

  @Autowired
  public PluginService(PluggableConfiguration configuration, DataSourceFactory dataSourceFactory)
      throws PluginException {
    this.configuration = configuration;
    this.dataSourceFactory = dataSourceFactory;
    this.availablePlugins = PluginDefaults.getDefaultPlugins();

    loadPlugins();
  }

  public <T extends Plugin> T getPlugin(Class<T> c) {
    return c.cast(availablePlugins.get(c.getSimpleName()));
  }

  private void loadPlugins() throws PluginException {
    try {
      if (configuration.isConfigured()) {
        for (Map.Entry<String, PluginConfig> p : configuration.getPlugins().entrySet()) {
          String key = capitalize(p.getKey());
          PluginConfig config = p.getValue();
          DataSource dataSource = getDataSource(config.getValue(PLUGIN_DATASOURCE_PARAMETER));

          Class<?> pluginClass = Class.forName(p.getValue().getType());
          Plugin plugin = (Plugin) pluginClass.getConstructor().newInstance();

          plugin.init(config, dataSource);

          availablePlugins.put(key, plugin);
        }
      }
    } catch (Exception e) {
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
}
