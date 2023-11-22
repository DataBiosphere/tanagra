package bio.terra.tanagra.app.configuration;

import bio.terra.common.db.BaseDatabaseProperties;
import bio.terra.tanagra.annotation.AnnotatedClass;
import bio.terra.tanagra.annotation.AnnotatedField;
import bio.terra.tanagra.annotation.AnnotatedInheritedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tanagra.db")
@AnnotatedClass(name = "Application Database", markdown = "Configure the application database.")
@AnnotatedInheritedField(
    name = "tanagra.db.uri",
    markdown = "URI of the application database.",
    environmentVariable = "TANAGRA_DB_URI",
    exampleValue = "jdbc:postgresql://127.0.0.1:5432/tanagra_db")
@AnnotatedInheritedField(
    name = "tanagra.db.username",
    markdown = "Username for the application database.",
    environmentVariable = "TANAGRA_DB_USERNAME",
    exampleValue = "dbuser")
@AnnotatedInheritedField(
    name = "tanagra.db.password",
    markdown = "Password for the application database.",
    environmentVariable = "TANAGRA_DB_PASSWORD",
    exampleValue = "dbpwd")
public class TanagraDatabaseProperties extends BaseDatabaseProperties {
  private static final Logger LOGGER = LoggerFactory.getLogger(TanagraDatabaseProperties.class);

  @AnnotatedField(
      name = "tanagra.db.initializeOnStart",
      markdown = "When true, the application database will be wiped on service startup.",
      environmentVariable = "TANAGRA_DB_INITIALIZE_ON_START",
      optional = true,
      defaultValue = "false")
  private boolean initializeOnStart;

  @AnnotatedField(
      name = "tanagra.db.upgradeOnStart",
      markdown =
          "When true, the application database will have Liquibase changesets applied on service startup.",
      environmentVariable = "TANAGRA_DB_UPGRADE_ON_START",
      optional = true,
      defaultValue = "false")
  private boolean upgradeOnStart;

  @AnnotatedField(
      name = "tanagra.db.cloudSqlInstance",
      markdown =
          "Name of the Cloud SQL instance `**project:region:instance**`. "
              + "Required to configure a CloudSQL connector (e.g. when deployed in AppEngine). "
              + "More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).",
      environmentVariable = "TANAGRA_DB_CLOUD_SQL_INSTANCE",
      optional = true)
  private String cloudSqlInstance;

  @AnnotatedField(
      name = "tanagra.db.socketFactory",
      markdown =
          "Name of the socket factory class. "
              + "Required to configure a CloudSQL connector (e.g. when deployed in AppEngine). "
              + "More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).",
      environmentVariable = "TANAGRA_DB_SOCKET_FACTORY",
      exampleValue = "com.google.cloud.sql.mysql.SocketFactory",
      optional = true)
  private String socketFactory;

  @AnnotatedField(
      name = "tanagra.db.driverClassName",
      markdown =
          "Name of the driver class. "
              + "Required to configure a CloudSQL connector (e.g. when deployed in AppEngine). "
              + "More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).",
      environmentVariable = "TANAGRA_DB_DRIVER_CLASS_NAME",
      exampleValue = "com.mysql.cj.jdbc.Driver",
      optional = true)
  private String driverClassName;

  @AnnotatedField(
      name = "tanagra.db.ipTypes",
      markdown =
          "Comma separated list of preferred IP types. "
              + "Used to configure a CloudSQL connector (e.g. when deployed in AppEngine). "
              + "Not required to use a CloudSQL connector. Leave empty to use GCP's default. "
              + "More information in [GCP documentation](https://cloud.google.com/sql/docs/mysql/connect-connectors#java).",
      environmentVariable = "TANAGRA_DB_DRIVER_IP_TYPES",
      exampleValue = "PUBLIC,PRIVATE",
      optional = true)
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
    // Don't log database password.
    LOGGER.info("Application database: username: {}", getUsername());
    LOGGER.info("Application database: cloud-sql-instance: {}", getCloudSqlInstance());
    LOGGER.info("Application database: socket-factory: {}", getSocketFactory());
    LOGGER.info("Application database: driver-class-name: {}", getDriverClassName());
    LOGGER.info("Application database: ip-types: {}", getIpTypes());
  }
}
