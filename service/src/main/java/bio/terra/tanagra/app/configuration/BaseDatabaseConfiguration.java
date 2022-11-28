package bio.terra.tanagra.app.configuration;

import bio.terra.common.db.BaseDatabaseProperties;
import bio.terra.common.db.DataSourceInitializer;
import javax.sql.DataSource;

/** Base class for accessing JDBC configuration properties. */
public class BaseDatabaseConfiguration {
  private final DataSource dataSource;

  public BaseDatabaseConfiguration(BaseDatabaseProperties databaseProperties) {
    dataSource = DataSourceInitializer.initializeDataSource(databaseProperties);
  }

  // The DataSource is nested inside this configuration, not directly injectable.
  public DataSource getDataSource() {
    return dataSource;
  }
}
