package bio.terra.tanagra.app.configuration;

import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
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
      markdown =
          "The maximum number of child threads a single request can spawn. "
              + "The application will only use multi-threading where it could improve performance, so just configuring "
              + "a specific number here is not a guarantee that exactly that many or even any child threads will be "
              + "spawned for a given request.\n\n "
              + "When unset, the application will default to using multi-threading where it could improve performance. "
              + "When set to 0, the application will only run things serially. "
              + "When set to some N > 0 (e.g. 2), the application may spawn at most N child threads.\n\n "
              + "(For export, spawning a single child thread would not improve performance, so 0 and 1 cause "
              + "identical behavior, i.e. run serially in same thread as request.)",
      environmentVariable = "TANAGRA_FEATURE_MAX_CHILD_THREADS",
      optional = true)
  private String maxChildThreads;

  public boolean isActivityLogEnabled() {
    return activityLogEnabled;
  }

  public Integer getMaxChildThreads() {
    try {
      return StringUtils.isEmpty(maxChildThreads) ? null : Integer.parseInt(maxChildThreads);
    } catch (NumberFormatException nfEx) {
      // Don't throw an exception here, which would prevent the service from starting up.
      LOGGER.warn("Invalid max child threads: {}", maxChildThreads);
      return null;
    }
  }

  public boolean hasMaxChildThreads() {
    return getMaxChildThreads() != null;
  }

  public void setActivityLogEnabled(boolean activityLogEnabled) {
    this.activityLogEnabled = activityLogEnabled;
  }

  public void setMaxChildThreads(String maxChildThreads) {
    this.maxChildThreads = maxChildThreads;
  }

  public void activityLogEnabledCheck() {
    if (!isActivityLogEnabled()) {
      throw new NotImplementedException("Activity log is not enabled");
    }
  }

  public void log() {
    LOGGER.info("Feature: activity-log-enabled: {}", isActivityLogEnabled());
    LOGGER.info("Feature: max-child-threads: {}", getMaxChildThreads());
  }
}
