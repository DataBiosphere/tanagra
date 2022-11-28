package bio.terra.tanagra.plugin;

import javax.sql.DataSource;

public interface Plugin {
  void init(PluginConfig config, DataSource dataSource);
}
