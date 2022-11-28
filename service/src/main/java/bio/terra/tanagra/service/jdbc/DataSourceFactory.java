package bio.terra.tanagra.service.jdbc;

import bio.terra.tanagra.app.configuration.JdbcConfiguration;
import bio.terra.tanagra.app.configuration.JdbcDataSourceConfiguration;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** A service for providing access to JDBC data sources. */
@Service
public class DataSourceFactory {
  private final ImmutableMap<DataSourceId, DataSource> dataSources;

  @Autowired
  public DataSourceFactory(JdbcConfiguration jdbcConfiguration) {
    dataSources =
        jdbcConfiguration.getDataSources().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    config -> new DataSourceId(config.getDataSourceId()),
                    JdbcDataSourceConfiguration::getDataSource));
  }

  /**
   * Returns the {@link DataSource} for the {@link DataSourceId}, or throws if no matching
   * DataSource is configured.
   */
  public DataSource getDataSource(DataSourceId dataSourceId) {
    DataSource dataSource = dataSources.get(dataSourceId);
    Preconditions.checkArgument(
        dataSource != null,
        "Unable to find DataSource configured for DataSourceId '%s'",
        dataSourceId.getId());
    return dataSource;
  }
}
