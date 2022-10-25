package bio.terra.tanagra.plugin;

import bio.terra.tanagra.service.jdbc.DataSourceFactory;

public interface IPlugin {
  void init(DataSourceFactory dataSoruceFactory, PluginConfig config);
}
