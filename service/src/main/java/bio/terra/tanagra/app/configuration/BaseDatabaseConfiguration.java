package bio.terra.tanagra.app.configuration;

import bio.terra.common.db.BaseDatabaseProperties;
import bio.terra.common.db.DataSourceInitializer;
import javax.sql.DataSource;
import java.util.Properties;

/** Base class for accessing JDBC configuration properties. */
public class BaseDatabaseConfiguration {
  private final DataSource dataSource;

  public BaseDatabaseConfiguration(BaseDatabaseProperties databaseProperties) {
    dataSource = DataSourceInitializer.initializeDataSource(databaseProperties);
  }

  // The DataSource is nested inside this configuration, not directly injectable.
  public DataSource getDataSource() {
    if (dataSource == null) {
      Properties props = new Properties();
      props.setProperty("user", getUsername());
      props.setProperty("password", getPassword());
      props.setProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
      props.setProperty("cloudSqlInstance", "all-of-us-workbench-test:us-central1:workbenchmaindb");

      ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:mysql:///tanagra_db", props);

      PoolableConnectionFactory poolableConnectionFactory =
              new PoolableConnectionFactory(connectionFactory, null);
      ObjectPool<PoolableConnection> connectionPool =
              new GenericObjectPool<>(poolableConnectionFactory);
      poolableConnectionFactory.setPool(connectionPool);
      dataSource = new PoolingDataSource<>(connectionPool);
    }
    return dataSource;
  }
}
