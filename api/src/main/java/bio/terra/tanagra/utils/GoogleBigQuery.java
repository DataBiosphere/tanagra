package bio.terra.tanagra.utils;

import bio.terra.tanagra.exception.SystemException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.JobStatistics;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for talking to Google BigQuery. This class maintains a singleton instance of the
 * BigQuery service, and a cache of table schemas to avoid looking up the schema for the same table
 * multiple times.
 */
public final class GoogleBigQuery {
  private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBigQuery.class);

  // default value for the maximum number of times to retry HTTP requests to BQ
  public static final int BQ_MAXIMUM_RETRIES = 5;
  private static final Duration MAX_QUERY_WAIT_TIME = Duration.ofSeconds(60);

  private final BigQuery bigQuery;
  private final Map<String, Schema> tableSchemasCache;

  public GoogleBigQuery(GoogleCredentials credentials, String projectId) {
    this.bigQuery =
        BigQueryOptions.newBuilder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .build()
            .getService();
    this.tableSchemasCache = new HashMap<>();
  }

  public Schema getTableSchemaWithCaching(String projectId, String datasetId, String tableId) {
    // check if the schema is in the cache
    String tablePath = TableId.of(projectId, datasetId, tableId).toString();
    Schema schema = tableSchemasCache.get(tablePath);

    // if it is, then just return it
    if (schema != null) {
      return schema;
    }

    // if it isn't, then fetch it, cache it, and then return it
    schema = getTableSchema(projectId, datasetId, tableId);
    tableSchemasCache.put(tablePath, schema);
    return schema;
  }

  public Schema getTableSchema(String projectId, String datasetId, String tableId) {
    Optional<Table> table = getTable(projectId, datasetId, tableId);
    if (table.isEmpty()) {
      throw new SystemException(
          "Table not found: " + projectId + ", " + datasetId + ", " + tableId);
    }
    return table.get().getDefinition().getSchema();
  }

  public Optional<Dataset> getDataset(String projectId, String datasetId) {
    try {
      DatasetId datasetPointer = DatasetId.of(projectId, datasetId);
      Dataset dataset =
          callWithRetries(() -> bigQuery.getDataset(datasetPointer), "Error looking up dataset");
      return Optional.ofNullable(dataset);
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public Optional<Table> getTable(String projectId, String datasetId, String tableId) {
    try {
      TableId tablePointer = TableId.of(projectId, datasetId, tableId);
      Table table =
          callWithRetries(
              () -> bigQuery.getTable(tablePointer), "Retryable error looking up table");
      return Optional.ofNullable(table);
    } catch (Exception e) {
      LOGGER.error("Error looking up table", e);
      return Optional.empty();
    }
  }

  /**
   * Create a new table from the results of a query.
   *
   * @param destinationTable the destination project+dataset+table id
   * @param query the SQL string
   * @param isDryRun true if this is a dry run and no table should actually be created
   * @return the result of the BQ query job
   */
  public TableResult createTableFromQuery(
      TableId destinationTable, String query, boolean isDryRun) {
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query)
            .setDestinationTable(destinationTable)
            .setDryRun(isDryRun)
            .build();

    if (isDryRun) {
      Job job = bigQuery.create(JobInfo.of(queryConfig));
      JobStatistics.QueryStatistics statistics = job.getStatistics();
      LOGGER.info(
          "BigQuery dry run performed successfully: {} bytes processed",
          statistics.getTotalBytesProcessed());
      return null;
    } else {
      return callWithRetries(
          () -> bigQuery.query(queryConfig), "Retryable error creating table from query");
    }
  }

  /**
   * Execute a query.
   *
   * @param query the query to run
   * @return the result of the BQ query
   * @throws InterruptedException from the bigQuery.query() method
   */
  public TableResult queryBigQuery(String query) {
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
    Job job = bigQuery.create(JobInfo.newBuilder(queryConfig).build());
    // TODO add pagination. Right now we always only return the first page of results.
    return callWithRetries(
        () ->
            job.getQueryResults(
                BigQuery.QueryResultsOption.maxWaitTime(MAX_QUERY_WAIT_TIME.toMillis())),
        "Error running BigQuery query: " + query);
  }

  /**
   * Utility method that checks if an exception thrown by the BQ client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  static boolean isRetryable(Exception ex) {
    if (ex instanceof SocketTimeoutException) {
      return true;
    }
    if (!(ex instanceof BigQueryException)) {
      return false;
    }
    LOGGER.error("Caught a BQ error.", ex);
    int statusCode = ((BigQueryException) ex).getCode();

    return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT

        // retry forbidden errors because we often see propagation delays when a user is just
        // granted access
        || statusCode == HttpStatus.SC_FORBIDDEN;
  }

  /**
   * Execute a function that includes hitting BQ endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the BQ client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the BQ client or the retries
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () ->
            HttpUtils.callWithRetries(
                makeRequest,
                GoogleBigQuery::isRetryable,
                BQ_MAXIMUM_RETRIES,
                HttpUtils.DEFAULT_DURATION_SLEEP_FOR_RETRY),
        errorMsg);
  }

  /**
   * Execute a function that includes hitting BQ endpoints. If an exception is thrown by the BQ
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the BQ client or the retries
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (IOException | InterruptedException ex) {
      // wrap the BQ exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }
}
