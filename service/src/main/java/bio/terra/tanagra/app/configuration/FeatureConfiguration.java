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
  private boolean activityLogEnabled;

  public boolean isArtifactStorageEnabled() {
    return artifactStorageEnabled;
  }

  public boolean isActivityLogEnabled() {
    return activityLogEnabled;
  }

  public void setArtifactStorageEnabled(boolean artifactStorageEnabled) {
    this.artifactStorageEnabled = artifactStorageEnabled;
  }

  public void setActivityLogEnabled(boolean activityLogEnabled) {
    this.activityLogEnabled = activityLogEnabled;
  }

  public void artifactStorageEnabledCheck() {
    if (!isArtifactStorageEnabled()) {
      throw new NotImplementedException("Artifact storage is not enabled");
    }
  }

  public void activityLogEnabledCheck() {
    if (!isActivityLogEnabled()) {
      throw new NotImplementedException("Activity log is not enabled");
    }
  }

  public void log() {
    LOGGER.info("Feature: artifact-storage-enabled: {}", isArtifactStorageEnabled());
    LOGGER.info("Feature: activity-log-enabled: {}", isActivityLogEnabled());
  }
}
