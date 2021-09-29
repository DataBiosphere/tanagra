package bio.terra.tanagra.workflow;

import org.apache.beam.sdk.io.jdbc.JdbcIO.DataSourceConfiguration;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;

/** {@link PipelineOptions} for connecting to a Cloud SQL Postgres instance. */
public interface CloudSqlOptions extends PipelineOptions {
  /**
   * The environment variable used to specify the password to use to connect to a CloudSQL instance.
   *
   * <p>Sensitive values like passwords should not be included as options, which are copied to logs.
   */
  String DB_PASSWORD_ENV = "CLOUDSQL_PWD";

  @Description("The full name of the CloudSQL instance: {projectId}:{region}:{instanceId}")
  String getCloudSqlInstanceName();

  void setCloudSqlInstanceName(String instanceName);

  @Description("The name of the CloudSQL database.")
  String getCloudSqlDatabaseName();

  void setCloudSqlDatabaseName(String databaseName);

  @Description("The username for connecting to the CloudSQL database.")
  String getCloudSqlUserName();

  void setCloudSqlUserName(String userName);

  /**
   * Create a {@link DataSourceConfiguration} for connectint to the Cloud SQL instance specified by
   * the options.
   */
  static DataSourceConfiguration createDataSourceConfiguration(CloudSqlOptions options) {
    // Don't encode sensitive information in options as they are written in job logs.
    String dbPassword = System.getenv(DB_PASSWORD_ENV);
    if (dbPassword == null || dbPassword.isEmpty()) {
      throw new IllegalArgumentException(
          "database password must be specified with environment variable " + DB_PASSWORD_ENV);
    }

    String connectionUrl =
        String.format(
            "jdbc:postgresql://google/%s"
                + "?cloudSqlInstance=%s"
                + "&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
                + "&user=%s"
                + "&password=%s",
            options.getCloudSqlDatabaseName(),
            options.getCloudSqlInstanceName(),
            options.getCloudSqlUserName(),
            dbPassword);
    return DataSourceConfiguration.create("org.postgresql.Driver", connectionUrl);
  }
}
