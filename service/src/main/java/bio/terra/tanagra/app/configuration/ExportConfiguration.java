package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
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

  private Shared shared;
  private List<PerModel> models;

  public Shared getShared() {
    return shared;
  }

  public void setShared(Shared shared) {
    this.shared = shared;
  }

  public List<PerModel> getModels() {
    return Collections.unmodifiableList(models);
  }

  public void setModels(List<PerModel> models) {
    this.models = models;
  }

  /** Write the data export flags into the log. Add an entry here for each new flag. */
  public void log() {
    LOGGER.info("Export: shared gcs-project-id: {}", shared.getGcsProjectId());
    LOGGER.info(
        "Export: shared gcs-bucket-names: {}",
        shared.getGcsBucketNames().stream().collect(Collectors.joining(",")));
    for (int i = 0; i < models.size(); i++) {
      PerModel m = models.get(i);
      LOGGER.info("Export: models[{}] name: {}", i, m.getName());
      LOGGER.info("Export: models[{}] display-name: {}", i, m.getDisplayName());
      LOGGER.info("Export: models[{}] type: {}", i, m.getType());
      LOGGER.info("Export: models[{}] redirect-away-url: {}", i, m.getRedirectAwayUrl());
      LOGGER.info(
          "Export: models[{}] params: {}",
          i,
          m.getParams().stream().collect(Collectors.joining(",")));
    }
  }

  @AnnotatedClass(
      name = "Export (Shared)",
      markdown = "Configure the export options shared by all models.")
  public static class Shared {
    @AnnotatedField(
        name = "tanagra.export.shared.gcsProjectId",
        markdown =
            "GCP project id that contains the GCS bucket(s) that all export models can use. "
                + "Required if there are any export models that need to write to GCS.",
        environmentVariable = "TANAGRA_EXPORT_SHARED_GCS_BUCKET_PROJECT_ID",
        optional = true,
        exampleValue = "broad-tanagra-dev")
    private String gcsProjectId;

    @AnnotatedField(
        name = "tanagra.export.shared.gcsBucketNames",
        markdown =
            "Comma separated list of all GCS bucket names that all export models can use. "
                + "Only include the bucket name, not the gs:// prefix. "
                + "Required if there are any export models that need to write to GCS.",
        environmentVariable = "TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES",
        optional = true,
        exampleValue = "broad-tanagra-dev-bq-export-uscentral1,broad-tanagra-dev-bq-export-useast1")
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

  @AnnotatedClass(
      name = "Export (Per Model)",
      markdown = "Configure the export options for each model.")
  public static class PerModel {
    @AnnotatedField(
        name = "tanagra.export.models.name",
        markdown =
            "Name of the export model. "
                + "This must be unique across all models for a given deployment. "
                + "Defaults to the name of the export model. "
                + "It's useful to override the default if you have more than one instance of the same model "
                + "(e.g. export to VWB parameterized with the dev environment URL, and another with the test environment URL).",
        environmentVariable =
            "TANAGRA_EXPORT_MODELS_0_NAME (Note 0 is the list index, so if you have 2 models, you will have 0 and 1 env vars.)",
        optional = true,
        exampleValue = "VWB_FILE_IMPORT_TO_DEV")
    private String name;

    private String displayName;
    private DataExport.Type type;
    private String redirectAwayUrl;
    private List<String> params;

    public String getName() {
      return name;
    }

    public String getDisplayName() {
      return displayName;
    }

    public DataExport.Type getType() {
      return type;
    }

    public String getRedirectAwayUrl() {
      return redirectAwayUrl;
    }

    public List<String> getParams() {
      return params == null ? Collections.emptyList() : Collections.unmodifiableList(params);
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public void setType(DataExport.Type type) {
      this.type = type;
    }

    public void setRedirectAwayUrl(String redirectAwayUrl) {
      this.redirectAwayUrl = redirectAwayUrl;
    }

    public void setParams(List<String> params) {
      this.params = params;
    }
  }
}
