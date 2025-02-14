package bio.terra.tanagra.service.export;

import bio.terra.tanagra.app.configuration.ExportConfiguration;
import org.apache.commons.lang3.StringUtils;

public record DataExportModel(String name, DataExport impl, ExportConfiguration.PerModel config) {
  public String getDisplayName() {
    String displayName = config.getDisplayName();
    if (StringUtils.isEmpty(displayName)) {
      displayName = impl.getDefaultDisplayName();
    }
    return displayName;
  }
}
