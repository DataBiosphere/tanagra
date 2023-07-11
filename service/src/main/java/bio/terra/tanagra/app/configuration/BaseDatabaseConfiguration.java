package bio.terra.tanagra.app.configuration;

import bio.terra.common.db.BaseDatabaseProperties;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/** Base class for accessing JDBC configuration properties. */
public class BaseDatabaseConfiguration {
  private final DataSource dataSource;

  public BaseDatabaseConfiguration(BaseDatabaseProperties baseDatabaseProperties) {
    //    dataSource = DataSourceInitializer.initializeDataSource(databaseProperties);
    Properties props = new Properties();
    props.setProperty("user", baseDatabaseProperties.getUsername());
    props.setProperty("password", baseDatabaseProperties.getPassword());
    props.setProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
    props.setProperty("cloudSqlInstance", "all-of-us-workbench-test:us-central1:workbenchmaindb");

    //    ConnectionFactory connectionFactory =
    //            new DriverManagerConnectionFactory(baseDatabaseProperties.getUri(), props);
    ConnectionFactory connectionFactory =
        new DriverManagerConnectionFactory("jdbc:mysql:///tanagra_db", props);

    PoolableConnectionFactory poolableConnectionFactory =
        new PoolableConnectionFactory(connectionFactory, null);

    GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
    config.setJmxEnabled(baseDatabaseProperties.isJmxEnabled());
    config.setMaxTotal(baseDatabaseProperties.getPoolMaxTotal());
    config.setMaxIdle(baseDatabaseProperties.getPoolMaxIdle());
    ObjectPool<PoolableConnection> connectionPool =
        new GenericObjectPool<>(poolableConnectionFactory, config);

    poolableConnectionFactory.setPool(connectionPool);

    dataSource = new PoolingDataSource<>(connectionPool);
  }

  // The DataSource is nested inside this configuration, not directly injectable.
  public DataSource getDataSource() {
    return dataSource;
  }
}
