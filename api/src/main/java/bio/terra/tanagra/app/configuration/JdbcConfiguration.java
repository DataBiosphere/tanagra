package bio.terra.tanagra.app.configuration;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "tanagra.jdbc")
public class JdbcConfiguration {

  /** The DataSources available for Tanagra to connect to. */
  private List<JdbcDataSourceConfiguration> dataSources = new ArrayList<>();

  public List<JdbcDataSourceConfiguration> getDataSources() {
    return dataSources;
  }

  public void setDataSources(List<JdbcDataSourceConfiguration> dataSources) {
    this.dataSources = dataSources;
  }
}
