package bio.terra.tanagra.service.databaseaccess;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.tanagra.service.databaseaccess.bigquery.BigQueryExecutor;
import bio.terra.tanagra.underlay.Table;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/** Provides a {@link QueryExecutor.Factory} bean. */
@Component
public class QueryExecutorFactoryManager {

  @Bean
  QueryExecutor.Factory factory() {
    return new FactoryImpl();
  }

  /** Implementation of {@link QueryExecutor.Factory} for general use. */
  private static class FactoryImpl implements QueryExecutor.Factory {
    @Override
    public QueryExecutor get(Table primaryTable) {
      // TODO support other databases.
      BigQuery bigQuery = createBigQueryClient(primaryTable.dataset().projectId());
      return new BigQueryExecutor(bigQuery);
    }

    private static BigQuery createBigQueryClient(String projectId) {
      GoogleCredentials credentials;
      try {
        // Explicitly grab credentials for clarity and better error reporting.
        credentials = GoogleCredentials.getApplicationDefault();
      } catch (IOException e) {
        throw new InternalServerErrorException("Unable to get Google credentials", e);
      }
      return BigQueryOptions.newBuilder()
          .setProjectId(projectId)
          .setCredentials(credentials)
          .build()
          .getService();
    }
  }
}
