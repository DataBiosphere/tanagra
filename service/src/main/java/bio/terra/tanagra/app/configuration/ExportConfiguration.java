package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.service.export.DataExport;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.export")
public class ExportConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExportConfiguration.class);

  private ExportInfraConfiguration commonInfra;
  private List<ExportModelConfiguration> models;

  public ExportInfraConfiguration getCommonInfra() {
    return commonInfra;
  }

  public void setCommonInfra(ExportInfraConfiguration commonInfra) {
    this.commonInfra = commonInfra;
  }

  public List<ExportModelConfiguration> getModels() {
    return Collections.unmodifiableList(models);
  }

  public void setModels(List<ExportModelConfiguration> models) {
    this.models = models;
  }

  /** Write the data export flags into the log. Add an entry here for each new flag. */
  public void logConfig() {
    LOGGER.info("Export common-infra gcs-project-id: {}", commonInfra.getGcsProjectId());
    LOGGER.info(
        "Export common-infra gcs-bucket-names: {}",
        commonInfra.getGcsBucketNames().stream().collect(Collectors.joining(",")));
    for (int i = 0; i < models.size(); i++) {
      ExportModelConfiguration m = models.get(i);
      LOGGER.info("Export models[{}] model: {}", i, m.getModel());
      LOGGER.info(
          "Export models[{}] params: {}",
          i,
          m.getParams().stream().collect(Collectors.joining(",")));
    }
  }

  public static class ExportInfraConfiguration {
    private String gcsProjectId;

    private List<String> gcsBucketNames;

    public String getGcsProjectId() {
      return gcsProjectId;
    }

    public void setGcsProjectId(String gcsProjectId) {
      this.gcsProjectId = gcsProjectId;
    }

    public List<String> getGcsBucketNames() {
      return Collections.unmodifiableList(gcsBucketNames);
    }

    public void setGcsBucketNames(List<String> gcsBucketNames) {
      this.gcsBucketNames = gcsBucketNames;
    }
  }

  public static class ExportModelConfiguration {
    private DataExport.Model model;
    private List<String> params;

    public DataExport.Model getModel() {
      return model;
    }

    public List<String> getParams() {
      return params == null ? Collections.emptyList() : Collections.unmodifiableList(params);
    }

    public void setModel(DataExport.Model model) {
      this.model = model;
    }

    public void setParams(List<String> params) {
      this.params = params;
    }
  }
}
