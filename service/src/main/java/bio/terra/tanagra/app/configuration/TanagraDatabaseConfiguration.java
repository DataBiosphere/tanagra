package bio.terra.tanagra.app.configuration;

import bio.terra.common.db.DataSourceInitializer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableConfigurationProperties(TanagraDatabaseProperties.class)
@EnableTransactionManagement
public class TanagraDatabaseConfiguration {
  private final DataSource dataSource;

  public TanagraDatabaseConfiguration(TanagraDatabaseProperties databaseProperties) {
    boolean useCloudSqlConnector =
        databaseProperties.getCloudSqlInstance() != null
            && !databaseProperties.getCloudSqlInstance().isEmpty();

    if (useCloudSqlConnector) {
      // Connect to application DB using CloudSQL connector (e.g. when deployed in AppEngine).
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(databaseProperties.getUri());
      config.setUsername(databaseProperties.getUsername());
      config.setPassword(databaseProperties.getPassword());
      config.setDriverClassName(databaseProperties.getDriverClassName());

      config.addDataSourceProperty("cloudSqlInstance", databaseProperties.getCloudSqlInstance());
      config.addDataSourceProperty("socketFactory", databaseProperties.getSocketFactory());

      if (databaseProperties.getIpTypes() != null && !databaseProperties.getIpTypes().isEmpty()) {
        config.addDataSourceProperty("ipTypes", databaseProperties.getIpTypes());
      }

      dataSource = new HikariDataSource(config);
    } else {
      // Connect to application DB via localhost (e.g. when deployed in GKE and using the CloudSQL
      // proxy sidecar container).
      dataSource = DataSourceInitializer.initializeDataSource(databaseProperties);
    }
  }

  @Bean("jdbcTemplate")
  public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
    return new NamedParameterJdbcTemplate(getDataSource());
  }

  // This bean plus the @EnableTransactionManagement annotation above enables the use of the
  // @Transaction annotation to control the transaction properties of the data source.
  @Bean("transactionManager")
  public PlatformTransactionManager getTransactionManager() {
    return new DataSourceTransactionManager(getDataSource());
  }

  public DataSource getDataSource() {
    return dataSource;
  }
}
