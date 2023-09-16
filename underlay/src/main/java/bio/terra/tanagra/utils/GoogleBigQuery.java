package bio.terra.tanagra.utils;

import bio.terra.tanagra.exception.SystemException;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.*;
import com.google.cloud.bigquery.storage.v1.TableName;
import com.google.protobuf.Descriptors;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
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
  private static final Duration MAX_QUERY_WAIT_TIME = Duration.ofMinutes(5);
  private static final org.threeten.bp.Duration MAX_BQ_CLIENT_TIMEOUT =
      org.threeten.bp.Duration.ofMinutes(5);

  private final GoogleCredentials credentials;
  private final BigQuery bigQuery;
  private final ConcurrentHashMap<String, Schema> tableSchemasCache;
  private final ConcurrentHashMap<String, Schema> querySchemasCache;

  public GoogleBigQuery(GoogleCredentials credentials, String projectId) {
    this.credentials = credentials;
    this.bigQuery =
        BigQueryOptions.newBuilder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .setRetrySettings(
                RetrySettings.newBuilder().setTotalTimeout(MAX_BQ_CLIENT_TIMEOUT).build())
            .build()
            .getService();
    this.tableSchemasCache = new ConcurrentHashMap<>();
    this.querySchemasCache = new ConcurrentHashMap<>();
  }

  public Schema getQuerySchemaWithCaching(String query) {
    // Check if the schema is in the cache.
    Schema schema = querySchemasCache.get(query);
    if (schema != null) {
      return schema;
    }

    // If it isn't, then fetch it and insert into the cache.
    schema = queryBigQuery(query).getSchema();
    querySchemasCache.put(query, schema);
    return schema;
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

  private Schema getTableSchema(String projectId, String datasetId, String tableId) {
    Optional<Table> table = getTable(projectId, datasetId, tableId);
    if (table.isEmpty()) {
      throw new SystemException(
          "Table not found: " + projectId + ", " + datasetId + ", " + tableId);
    }
    return table.get().getDefinition().getSchema();
  }

  public Optional<Dataset> getDataset(
      String projectId, String datasetId, BigQuery.DatasetOption... datasetOptions) {
    try {
      DatasetId datasetPointer = DatasetId.of(projectId, datasetId);
      Dataset dataset =
          callWithRetries(
              () -> bigQuery.getDataset(datasetPointer, datasetOptions),
              "Error looking up dataset");
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
   * Create a new empty table from a schema.
   *
   * @param destinationTable the destination project+dataset+table id
   * @param schema the table schema
   * @param isDryRun true if this is a dry run and no table should actually be created
   * @return the result of the BQ query job
   */
  public Table createTableFromSchema(
      TableId destinationTable, Schema schema, @Nullable Clustering clustering, boolean isDryRun) {
    StandardTableDefinition.Builder tableDefn =
        StandardTableDefinition.newBuilder().setSchema(schema);
    if (clustering != null) {
      tableDefn.setClustering(clustering);
    }
    TableInfo tableInfo = TableInfo.of(destinationTable, tableDefn.build());
    LOGGER.info("schema: {}", schema);
    if (isDryRun) {
      // TODO: Can we validate the schema here or something?
      return null;
    } else {
      return callWithRetries(
          () -> bigQuery.create(tableInfo), "Retryable error creating table from schema");
    }
  }

  /**
   * Create a new table from the results of a query.
   *
   * @param query the SQL string
   * @param destinationTable the destination project+dataset+table id
   * @param isDryRun true if this is a dry run and no table should actually be created
   * @return the result of the BQ query job
   */
  public TableResult createTableFromQuery(
      TableId destinationTable, String query, @Nullable Clustering clustering, boolean isDryRun) {
    QueryJobConfiguration.Builder queryJobConfig = QueryJobConfiguration.newBuilder(query)
            .setDestinationTable(destinationTable)
            .setDryRun(isDryRun);
    if (clustering != null) {
      queryJobConfig.setClustering(clustering);
    }
    return runUpdateQuery(queryJobConfig.build(), isDryRun);
  }

  /**
   * Run an insert or update query.
   *
   * @param query the SQL string
   * @param isDryRun true if this is a dry run and no table should actually be created
   * @return the result of the BQ query job
   */
  public TableResult runInsertUpdateQuery(String query, boolean isDryRun) {
    return runUpdateQuery(
        QueryJobConfiguration.newBuilder(query).setDryRun(isDryRun).build(), isDryRun);
  }

  private TableResult runUpdateQuery(QueryJobConfiguration queryConfig, boolean isDryRun) {
    if (isDryRun) {
      Job job = bigQuery.create(JobInfo.of(queryConfig));
      JobStatistics.QueryStatistics statistics = job.getStatistics();
      LOGGER.info(
          "BigQuery dry run performed successfully: {} bytes processed",
          statistics.getTotalBytesProcessed());
      return null;
    } else {
      return callWithRetries(() -> bigQuery.query(queryConfig), "Retryable error running query");
    }
  }

  public TableResult queryBigQuery(String query) {
    return queryBigQuery(query, null, null);
  }

  /**
   * Execute a query.
   *
   * @param query the query to run
   * @return the result of the BQ query
   * @throws InterruptedException from the bigQuery.query() method
   */
  public TableResult queryBigQuery(
      String query, @Nullable String pageToken, @Nullable Integer pageSize) {
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build();
    Job job = bigQuery.create(JobInfo.newBuilder(queryConfig).build());

    List<BigQuery.QueryResultsOption> queryResultsOptions = new ArrayList<>();
    queryResultsOptions.add(
        BigQuery.QueryResultsOption.maxWaitTime(MAX_QUERY_WAIT_TIME.toMillis()));
    if (pageToken != null) {
      queryResultsOptions.add(BigQuery.QueryResultsOption.pageToken(pageToken));
    }
    if (pageSize != null) {
      queryResultsOptions.add(BigQuery.QueryResultsOption.pageSize(pageSize));
    }

    return callWithRetries(
        () -> job.getQueryResults(queryResultsOptions.toArray(new BigQuery.QueryResultsOption[0])),
        "Error running BigQuery query: " + query);
  }

  /**
   * Delete a table. Do nothing if the table is not found (i.e. assume that means it's already
   * deleted).
   */
  public void deleteTable(String projectId, String datasetId, String tableId) {
    try {
      TableId tablePointer = TableId.of(projectId, datasetId, tableId);
      boolean deleteSuccessful =
          callWithRetries(() -> bigQuery.delete(tablePointer), "Retryable error deleting table");
      if (deleteSuccessful) {
        LOGGER.info("Table deleted: {}, {}, {}", projectId, datasetId, tableId);
      } else {
        LOGGER.info("Table not found: {}, {}, {}", projectId, datasetId, tableId);
      }
    } catch (Exception ex) {
      throw new SystemException("Error deleting table", ex);
    }
  }

  /** Append rows to a table using the Storage Write API. */
  public void insertWithStorageWriteApi(
      String projectId, String datasetId, String tableId, List<JSONObject> records) {
    try {
      BigQueryStorageWriteApi.insertWithStorageWriteApi(
          FixedCredentialsProvider.create(credentials),
          TableName.of(projectId, datasetId, tableId),
          records);
    } catch (IOException | InterruptedException | Descriptors.DescriptorValidationException ex) {
      throw new SystemException("Error inserting rows with Storage Write API", ex);
    }
  }

  public int getNumRows(String projectId, String datasetId, String tableId) {
    String queryRowCount =
        "SELECT COUNT(*) FROM `" + projectId + "." + datasetId + "." + tableId + "`";
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryRowCount).build();
    TableResult results =
        callWithRetries(
            () -> bigQuery.query(queryConfig), "Error counting rows in BigQuery table: " + tableId);
    return Integer.parseInt(results.getValues().iterator().next().get("f0_").getStringValue());
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
