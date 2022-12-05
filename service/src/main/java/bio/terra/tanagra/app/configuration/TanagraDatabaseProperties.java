package bio.terra.tanagra.app.configuration;

import bio.terra.common.db.BaseDatabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tanagra.db")
public class TanagraDatabaseProperties extends BaseDatabaseProperties {
  private static final Logger LOGGER = LoggerFactory.getLogger(TanagraDatabaseProperties.class);

  /** If true, primary database will be wiped */
  private boolean initializeOnStart;
  /** If true, primary database will have changesets applied */
  private boolean upgradeOnStart;

  public boolean isInitializeOnStart() {
    return initializeOnStart;
  }

  public void setInitializeOnStart(boolean initializeOnStart) {
    this.initializeOnStart = initializeOnStart;
  }

  public boolean isUpgradeOnStart() {
    return upgradeOnStart;
  }

  public void setUpgradeOnStart(boolean upgradeOnStart) {
    this.upgradeOnStart = upgradeOnStart;
  }

  /** Write the properties into the log. Add an entry here for each new property. */
  public void logFlags() {
    LOGGER.info("Database flag: initialize-on-start: {}", isInitializeOnStart());
    LOGGER.info("Database flag: upgrade-on-start: {}", isUpgradeOnStart());
  }
}
