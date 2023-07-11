package bio.terra.tanagra.app.configuration;

import java.util.Properties;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

/** A POJO to use in Spring Configurations for configuring access to JDBC data sources. */
public class JdbcDataSourceConfiguration {
  /** An id within Tanagra for this DataSource. This is referenced by Underlays. */
  private String dataSourceId;
  /** JDBC URL of the database, e.g. 'jdbc:postgresql://127.0.0.1:5432/my_database' */
  private String url;
  /** Username for the database */
  private String username;
  /** Password for the database */
  private String password;

  /** Lazy pooled data source instance for connecting to the JDBC data source. */
  private PoolingDataSource<PoolableConnection> dataSource;

  public String getDataSourceId() {
    return dataSourceId;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public void setDataSourceId(String dataSourceId) {
    this.dataSourceId = dataSourceId;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  // Main use of the configuration is this pooling data source object.
  public PoolingDataSource<PoolableConnection> getDataSource() {
    // Lazy allocation of the data source
    if (dataSource == null) {
      configureDataSource();
    }
    return dataSource;
  }

  private void configureDataSource() {
    Properties props = new Properties();
    props.setProperty("user", getUsername());
    props.setProperty("password", getPassword());

    ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(getUrl(), props);

    PoolableConnectionFactory poolableConnectionFactory =
            new PoolableConnectionFactory(connectionFactory, null);

    ObjectPool<PoolableConnection> connectionPool =
            new GenericObjectPool<>(poolableConnectionFactory);

    poolableConnectionFactory.setPool(connectionPool);

    dataSource = new PoolingDataSource<>(connectionPool);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("uri", url)
        .append("username", username)
        // NOTE: password is not printed; that avoids it showing up in logs
        .toString();
  }
}
