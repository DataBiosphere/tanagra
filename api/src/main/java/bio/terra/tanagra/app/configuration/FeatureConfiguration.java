package bio.terra.tanagra.app.configuration;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.feature")
public class FeatureConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureConfiguration.class);

  private boolean artifactStorageEnabled;

  public boolean isArtifactStorageEnabled() {
    return artifactStorageEnabled;
  }

  public void setArtifactStorageEnabled(boolean artifactStorageEnabled) {
    this.artifactStorageEnabled = artifactStorageEnabled;
  }

  public void artifactStorageEnabledCheck() {
    if (!isArtifactStorageEnabled()) {
      throw new NotImplementedException("Artifact storage is not enabled");
    }
  }

  /**
   * Write the feature settings into the log
   *
   * <p>Add an entry here for each new feature
   */
  public void logFeatures() {
    LOGGER.info("Feature: artifact-storage-enabled: {}", isArtifactStorageEnabled());
  }
}
