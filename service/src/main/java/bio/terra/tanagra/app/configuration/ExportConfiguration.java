package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import bio.terra.tanagra.service.export.DataExport;
import java.util.Collections;
import java.util.List;
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
    LOGGER.info("Export: shared gcs-project-id: {}", shared.getGcpProjectId());
    LOGGER.info(
        "Export: shared gcs-bucket-names: {}", String.join(",", shared.getGcsBucketNames()));
    for (int i = 0; i < models.size(); i++) {
      PerModel m = models.get(i);
      LOGGER.info("Export: models[{}] name: {}", i, m.getName());
      LOGGER.info("Export: models[{}] display-name: {}", i, m.getDisplayName());
      LOGGER.info("Export: models[{}] type: {}", i, m.getType());
      LOGGER.info("Export: models[{}] redirect-away-url: {}", i, m.getRedirectAwayUrl());
      LOGGER.info("Export: models[{}] num-primary-entity-cap: {}", i, m.getNumPrimaryEntityCap());
      LOGGER.info("Export: models[{}] params: {}", i, String.join(",", m.getParams()));
    }
  }

  @AnnotatedClass(
      name = "Export (Shared)",
      markdown = "Configure the export options shared by all models.")
  public static class Shared {
    @AnnotatedField(
        name = "tanagra.export.shared.gcpProjectId",
        markdown =
            "GCP project id that contains the BQ dataset and GCS bucket(s) that all export models can use. "
                + "Required if there are any export models that need to export from BQ to GCS.",
        environmentVariable = "TANAGRA_EXPORT_SHARED_GCP_PROJECT_ID",
        optional = true,
        exampleValue = "broad-tanagra-dev")
    private String gcpProjectId;

    @AnnotatedField(
        name = "tanagra.export.shared.bqDatasetIds",
        markdown =
            "Comma separated list of all BQ dataset ids that all export models can use. "
                + "Required if there are any export models that need to export from BQ to GCS.",
        environmentVariable = "TANAGRA_EXPORT_SHARED_BQ_DATASET_IDS",
        optional = true,
        exampleValue = "service_export_us,service_export_uscentral1")
    private List<String> bqDatasetIds;

    @AnnotatedField(
        name = "tanagra.export.shared.gcsBucketNames",
        markdown =
            "Comma separated list of all GCS bucket names that all export models can use. "
                + "Only include the bucket name, not the gs:// prefix. "
                + "Required if there are any export models that need to write to GCS.",
        environmentVariable = "TANAGRA_EXPORT_SHARED_GCS_BUCKET_NAMES",
        optional = true,
        exampleValue = "bq-export-uscentral1,bq-export-useast1")
    private List<String> gcsBucketNames;

    public String getGcpProjectId() {
      return gcpProjectId;
    }

    public void setGcpProjectId(String gcpProjectId) {
      this.gcpProjectId = gcpProjectId;
    }

    public List<String> getBqDatasetIds() {
      return bqDatasetIds;
    }

    public void setBqDatasetIds(List<String> bqDatasetIds) {
      this.bqDatasetIds = bqDatasetIds;
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
                + "(e.g. export to workbench parameterized with the dev environment URL, and another parameterized "
                + "with the test environment URL).",
        environmentVariable =
            "TANAGRA_EXPORT_MODELS_0_NAME (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)",
        optional = true,
        exampleValue = "VWB_FILE_EXPORT_TO_DEV")
    private String name;

    @AnnotatedField(
        name = "tanagra.export.models.displayName",
        markdown =
            "Displayed name of the export model. "
                + "This is for display only and will be shown in the export dialog when the user "
                + "initiates an export. Defaults to the display name provided by the export model. "
                + "It's useful to override the default if you have more than one instance of the same model "
                + "(e.g. export to workbench parameterized with the dev environment URL, and another parameterized "
                + "with the test environment URL).",
        environmentVariable =
            "TANAGRA_EXPORT_MODELS_0_DISPLAY_NAME (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)",
        optional = true,
        exampleValue = "Export File to Workbench (dev instance)")
    private String displayName;

    @AnnotatedField(
        name = "tanagra.export.models.type",
        markdown =
            "Pointer to the data export model Java class. Currently this must be one of the enum values in the"
                + "`bio.terra.tanagra.service.export.DataExport.Type` Java class. In the future, "
                + "it will support arbitrary class names",
        environmentVariable =
            "TANAGRA_EXPORT_MODELS_0_TYPE (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)",
        optional = true,
        exampleValue = "IPYNB_FILE_DOWNLOAD")
    private DataExport.Type type;

    @AnnotatedField(
        name = "tanagra.export.models.redirectAwayUrl",
        markdown =
            "URL to redirect the user to once the Tanagra export model has run. "
                + "This is useful when you want to import a file to another site. e.g. Write the exported data "
                + "to CSV files in GCS and then redirect to a workbench URL, passing the URL to the CSV files so "
                + "the workbench can import them somewhere.",
        environmentVariable =
            "TANAGRA_EXPORT_MODELS_0_REDIRECT_AWAY_URL (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)",
        optional = true,
        exampleValue =
            "https://terra-devel-ui-terra.api.verily.com/import?urlList=${tsvFileUrl}&returnUrl=${redirectBackUrl}&returnApp=${sourceApp}")
    private String redirectAwayUrl;

    @AnnotatedField(
        name = "tanagra.export.models.numPrimaryEntityCap",
        markdown =
            "Maximum number of primary entity instances to allow exporting (e.g. number of persons <= 10k). "
                + "This is useful when you want to limit the amount of data a user can export "
                + "e.g. to keep file sizes reasonable. The limit is inclusive, so 10k means <=10k is allowed. "
                + "Note that this limit applies to the union of all selected cohorts, not each cohort individually. "
                + "When unset, there is no default cap. This export model will always run, regardless of how many "
                + "primary entity instances are included in the selected cohorts.",
        environmentVariable =
            "TANAGRA_EXPORT_MODELS_0_NUM_PRIMARY_ENTITY_CAP (Note 0 is the list index, so if you have 2 models, you'd have 0 and 1 env vars.)",
        optional = true,
        exampleValue = "10000")
    private String numPrimaryEntityCap;

    @AnnotatedField(
        name = "tanagra.export.models.params",
        markdown =
            "Map of parameters to pass to the export model. This is useful when you want to parameterize a "
                + "model beyond just the redirect URL. e.g. A description for a generated notebook file.",
        environmentVariable =
            "TANAGRA_EXPORT_MODELS_0_PARAMS_0 (Note the first 0 is the list index of the export models, so "
                + "if you have 2 models, you'd have 0 and 1 env vars. The second 0 is the list index of "
                + "the parameters, so if you have 2 parameters, you'd need 0 and 1 env vars.)",
        optional = true,
        exampleValue = "Notebook file generated for Workbench v35")
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

    public Integer getNumPrimaryEntityCap() {
      try {
        return numPrimaryEntityCap == null ? null : Integer.parseInt(numPrimaryEntityCap);
      } catch (NumberFormatException nfEx) {
        // Don't throw an exception here, which would prevent the service from starting up.
        LOGGER.warn("Invalid num primary entity cap: {}", numPrimaryEntityCap);
        return null;
      }
    }

    public boolean hasNumPrimaryEntityCap() {
      return getNumPrimaryEntityCap() != null;
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

    public void setNumPrimaryEntityCap(String numPrimaryEntityCap) {
      this.numPrimaryEntityCap = numPrimaryEntityCap;
    }

    public void setParams(List<String> params) {
      this.params = params;
    }
  }
}
