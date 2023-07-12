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

  private String cloudSqlInstance;
  private String socketFactory;
  private String driverClassName;
  private String ipTypes;

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

  public String getCloudSqlInstance() {
    return cloudSqlInstance;
  }

  public void setCloudSqlInstance(String cloudSqlInstance) {
    this.cloudSqlInstance = cloudSqlInstance;
  }

  public String getSocketFactory() {
    return socketFactory;
  }

  public void setSocketFactory(String socketFactory) {
    this.socketFactory = socketFactory;
  }

  public String getDriverClassName() {
    return driverClassName;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public String getIpTypes() {
    return ipTypes;
  }

  public void setIpTypes(String ipTypes) {
    this.ipTypes = ipTypes;
  }

  /** Write the properties into the log. Add an entry here for each new property. */
  public void logFlags() {
    LOGGER.info("Database flag: initialize-on-start: {}", isInitializeOnStart());
    LOGGER.info("Database flag: upgrade-on-start: {}", isUpgradeOnStart());
    LOGGER.info("Database flag: uri: {}", getUri());
    LOGGER.info("Database flag: username: {}", getUsername());
    LOGGER.info("Database flag: cloud-sql-instance: {}", getCloudSqlInstance());
    LOGGER.info("Database flag: socket-factory: {}", getSocketFactory());
    LOGGER.info("Database flag: driver-class-name: {}", getDriverClassName());
    LOGGER.info("Database flag: ip-types: {}", getIpTypes());
  }
}
