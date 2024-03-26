package bio.terra.tanagra.service.export;

import bio.terra.tanagra.app.configuration.ExportConfiguration;

public class DataExportModel {
  private final String name;
  private final DataExport impl;
  private final ExportConfiguration.PerModel config;

  public DataExportModel(String name, DataExport impl, ExportConfiguration.PerModel config) {
    this.name = name;
    this.impl = impl;
    this.config = config;
  }

  public String getName() {
    return name;
  }

  public DataExport getImpl() {
    return impl;
  }

  public ExportConfiguration.PerModel getConfig() {
    return config;
  }

  public String getDisplayName() {
    String displayName = config.getDisplayName();
    if (displayName == null || displayName.isEmpty()) {
      displayName = impl.getDefaultDisplayName();
    }
    return displayName;
  }
}
