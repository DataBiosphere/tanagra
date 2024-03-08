package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.feature")
@AnnotatedClass(name = "Feature Flags", markdown = "Enable and disable specific features.")
public class FeatureConfiguration {
  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureConfiguration.class);

  @AnnotatedField(
      name = "tanagra.feature.artifactStorageEnabled",
      markdown =
          "When true, artifacts can be created, updated and deleted. "
              + "Artifacts include studies, cohorts, concept sets, reviews, and annotations.",
      environmentVariable = "TANAGRA_FEATURE_ARTIFACT_STORAGE_ENABLED",
      optional = true,
      defaultValue = "false")
  private boolean artifactStorageEnabled;

  @AnnotatedField(
      name = "tanagra.feature.activityLogEnabled",
      markdown =
          "When true, we store activity log events in the application database. "
              + "This is intended to support auditing requirements.",
      environmentVariable = "TANAGRA_FEATURE_ACTIVITY_LOG_ENABLED",
      optional = true,
      defaultValue = "false")
  private boolean activityLogEnabled;

  @AnnotatedField(
      name = "tanagra.feature.maxChildThreads",
      markdown = "The maximum number of child threads a single request can spawn.",
      environmentVariable = "TANAGRA_FEATURE_MAX_CHILD_THREADS",
      optional = true)
  private String maxChildThreads;

  public boolean isArtifactStorageEnabled() {
    return artifactStorageEnabled;
  }

  public boolean isActivityLogEnabled() {
    return activityLogEnabled;
  }

  public Integer getMaxChildThreads() {
    try {
      return Integer.parseInt(maxChildThreads);
    } catch (NumberFormatException nfEx) {
      // Don't throw an exception here, which would prevent the service from starting up.
      LOGGER.warn("Invalid max child threads: {}", maxChildThreads);
      return null;
    }
  }

  public boolean hasMaxChildThreads() {
    return getMaxChildThreads() != null;
  }

  public void setArtifactStorageEnabled(boolean artifactStorageEnabled) {
    this.artifactStorageEnabled = artifactStorageEnabled;
  }

  public void setActivityLogEnabled(boolean activityLogEnabled) {
    this.activityLogEnabled = activityLogEnabled;
  }

  public void setMaxChildThreads(String maxChildThreads) {
    this.maxChildThreads = maxChildThreads;
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
    LOGGER.info("Feature: max-child-threads: {}", getMaxChildThreads());
  }
}
