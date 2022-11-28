package bio.terra.tanagra.plugin;

import javax.sql.DataSource;

public interface IPlugin {
  void init(PluginConfig config, DataSource dataSource);
}
