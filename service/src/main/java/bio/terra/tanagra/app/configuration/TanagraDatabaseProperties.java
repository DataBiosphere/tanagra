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

  public void log() {
    LOGGER.info("Application database: initialize-on-start: {}", isInitializeOnStart());
    LOGGER.info("Application database: upgrade-on-start: {}", isUpgradeOnStart());
    LOGGER.info("Application database: uri: {}", getUri());
    LOGGER.info("Application database: username: {}", getUsername());
    LOGGER.info("Application database: cloud-sql-instance: {}", getCloudSqlInstance());
    LOGGER.info("Application database: socket-factory: {}", getSocketFactory());
    LOGGER.info("Application database: driver-class-name: {}", getDriverClassName());
    LOGGER.info("Application database: ip-types: {}", getIpTypes());
  }
}
