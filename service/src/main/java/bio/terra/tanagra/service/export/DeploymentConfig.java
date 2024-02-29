package bio.terra.tanagra.service.export;

import bio.terra.tanagra.app.configuration.ExportConfiguration;
import java.util.Collections;
import java.util.List;

public final class DeploymentConfig {
  private final Shared shared;
  private final String redirectAwayUrl;
  private final List<String> params;

  private DeploymentConfig(Shared shared, String redirectAwayUrl, List<String> params) {
    this.shared = shared;
    this.redirectAwayUrl = redirectAwayUrl;
    this.params = params;
  }

  public static DeploymentConfig fromApplicationConfig(
      ExportConfiguration.Shared appSharedConfig, ExportConfiguration.PerModel appModelConfig) {
    return new DeploymentConfig(
        Shared.fromApplicationConfig(appSharedConfig),
        appModelConfig.getRedirectAwayUrl(),
        appModelConfig.getParams());
  }

  public Shared getShared() {
    return shared;
  }

  public String getRedirectAwayUrl() {
    return redirectAwayUrl;
  }

  public List<String> getParams() {
    return Collections.unmodifiableList(params);
  }

  public static final class Shared {
    private final String gcpProjectId;
    private final List<String> bqDatasetIds;
    private final List<String> gcsBucketNames;

    private Shared(String gcpProjectId, List<String> bqDatasetIds, List<String> gcsBucketNames) {
      this.gcpProjectId = gcpProjectId;
      this.bqDatasetIds = bqDatasetIds;
      this.gcsBucketNames = gcsBucketNames;
    }

    public static Shared fromApplicationConfig(ExportConfiguration.Shared appConfig) {
      return new Shared(
          appConfig.getGcpProjectId(), appConfig.getBqDatasetIds(), appConfig.getGcsBucketNames());
    }

    public String getGcpProjectId() {
      return gcpProjectId;
    }

    public List<String> getBqDatasetIds() {
      return Collections.unmodifiableList(bqDatasetIds);
    }

    public List<String> getGcsBucketNames() {
      return Collections.unmodifiableList(gcsBucketNames);
    }
  }
}
