package bio.terra.tanagra.utils;

import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import com.google.api.gax.retrying.RetrySettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Clustering;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.ExtractJobConfiguration;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.JobStatistics;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.bigquery.TableResult;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for talking to Google BigQuery. */
public final class GoogleBigQuery {
  private static final Logger LOGGER = LoggerFactory.getLogger(GoogleBigQuery.class);

  // Default value for the maximum number of times to retry HTTP requests.
  public static final int BQ_MAXIMUM_RETRIES = 5;
  private static final Duration MAX_QUERY_WAIT_TIME = Duration.ofMinutes(10);
  private static final org.threeten.bp.Duration MAX_BQ_CLIENT_TIMEOUT =
      org.threeten.bp.Duration.ofMinutes(10);

  private final BigQuery bigQuery;

  private GoogleBigQuery(GoogleCredentials credentials, String projectId) {
    this.bigQuery =
        BigQueryOptions.newBuilder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .setRetrySettings(
                RetrySettings.newBuilder().setTotalTimeout(MAX_BQ_CLIENT_TIMEOUT).build())
            .build()
            .getService();
  }

  public static GoogleBigQuery forApplicationDefaultCredentials(String projectId) {
    GoogleCredentials credentials;
    try {
      credentials = GoogleCredentials.getApplicationDefault();
    } catch (IOException ioEx) {
      throw new SystemException("Error loading application default credentials", ioEx);
    }
    return new GoogleBigQuery(credentials, projectId);
  }

  // -----------------------------------------------------------------------------------
  // Datasets
  private Optional<Dataset> getDataset(
      String projectId, String datasetId, BigQuery.DatasetOption... datasetOptions) {
    try {
      DatasetId datasetPointer = DatasetId.of(projectId, datasetId);
      Dataset dataset =
          callWithRetries(
              () -> bigQuery.getDataset(datasetPointer, datasetOptions),
              "Error looking up dataset");
      return Optional.ofNullable(dataset);
    } catch (Exception ex) {
      LOGGER.warn("Error looking up dataset", ex);
      return Optional.empty();
    }
  }

  public String findDatasetWithLocation(
      String gcpProjectId, List<String> bqDatasetIds, String bigQueryDataLocation) {
    // Lookup the BQ dataset location. Return the first dataset with a compatible location for the
    // dataset.
    for (String datasetId : bqDatasetIds) {
      Optional<Dataset> dataset =
          getDataset(
              gcpProjectId,
              datasetId,
              BigQuery.DatasetOption.fields(BigQuery.DatasetField.LOCATION));
      if (dataset.isEmpty()) {
        LOGGER.warn("Dataset not found: {}", datasetId);
        continue;
      }
      String datasetLocation = dataset.get().getLocation();
      if (bigQueryDataLocation.equalsIgnoreCase(datasetLocation)) {
        return datasetId;
      }
    }
    throw new SystemException(
        "No compatible BQ dataset found for export from BQ dataset in location: "
            + bigQueryDataLocation);
  }

  // -----------------------------------------------------------------------------------
  // Tables
  public Optional<Table> getTable(String projectId, String datasetId, String tableId) {
    try {
      TableId tablePointer = TableId.of(projectId, datasetId, tableId);
      Table table =
          callWithRetries(
              () -> bigQuery.getTable(tablePointer), "Retryable error looking up table");
      return Optional.ofNullable(table);
    } catch (Exception ex) {
      LOGGER.warn("Error looking up table", ex);
      return Optional.empty();
    }
  }

  public Table pollForTableExistenceOrThrow(
      String projectId, String datasetId, String tableId, int maxCalls, Duration sleepDuration) {
    try {
      Optional<Table> table =
          RetryUtils.pollWithRetries(
              () -> getTable(projectId, datasetId, tableId),
              checkTableExistsResult -> checkTableExistsResult.isPresent(),
              ex -> false,
              maxCalls,
              sleepDuration);
      if (table.isEmpty()) {
        throw new SystemException(
            "Error finding table "
                + tableId
                + ". Polling timed out after "
                + maxCalls
                + " tries, sleeping "
                + sleepDuration.toString()
                + " between each try.");
      }
      return table.get();
    } catch (InterruptedException intEx) {
      throw new SystemException("Error polling for table existence", intEx);
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

  public Job exportTableToGcs(
      TableId sourceTable, String destinationUrl, String compression, String fileFormat) {
    ExtractJobConfiguration extractConfig =
        ExtractJobConfiguration.newBuilder(sourceTable, destinationUrl)
            .setCompression(compression)
            .setFormat(fileFormat)
            .build();

    // Blocks until this job completes its execution, either failing or succeeding.
    return callWithRetries(
        () -> {
          Job job = bigQuery.create(JobInfo.of(extractConfig));
          return job.waitFor();
        },
        "Retryable error running query");
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

  // -----------------------------------------------------------------------------------
  // Queries
  public TableResult runQuery(
      String sql,
      @Nullable Map<String, QueryParameterValue> queryParams,
      @Nullable String pageToken,
      @Nullable Integer pageSize,
      @Nullable TableId destinationTable,
      @Nullable Clustering clustering) {
    Pair<QueryJobConfiguration, List<BigQuery.QueryResultsOption>> queryJobConfig =
        buildQueryJobConfig(
            sql, false, queryParams, pageToken, pageSize, destinationTable, clustering);
    return callWithRetries(
        () -> {
          Job job = bigQuery.create(JobInfo.newBuilder(queryJobConfig.getLeft()).build());
          TableResult tableResult =
              job.getQueryResults(
                  queryJobConfig.getRight().toArray(new BigQuery.QueryResultsOption[0]));
          LOGGER.info("SQL query returns {} rows across all pages", tableResult.getTotalRows());
          return tableResult;
        },
        "Error running query: " + queryJobConfig.getLeft().getQuery());
  }

  public JobStatistics.QueryStatistics dryRunQuery(
      String sql,
      @Nullable Map<String, QueryParameterValue> queryParams,
      @Nullable String pageToken,
      @Nullable Integer pageSize,
      @Nullable TableId destinationTable,
      @Nullable Clustering clustering) {
    Pair<QueryJobConfiguration, List<BigQuery.QueryResultsOption>> queryJobConfig =
        buildQueryJobConfig(
            sql, true, queryParams, pageToken, pageSize, destinationTable, clustering);
    return callWithRetries(
        () -> {
          Job job = bigQuery.create(JobInfo.newBuilder(queryJobConfig.getLeft()).build());
          JobStatistics.QueryStatistics queryStatistics = job.getStatistics();
          LOGGER.info(
              "SQL dry run: statementType={}, cacheHit={}, totalBytesProcessed={}, totalSlotMs={}",
              queryStatistics.getStatementType(),
              queryStatistics.getCacheHit(),
              queryStatistics.getTotalBytesProcessed(),
              queryStatistics.getTotalSlotMs());
          return queryStatistics;
        },
        "Error getting job statistics for query: " + queryJobConfig.getLeft().getQuery());
  }

  private Pair<QueryJobConfiguration, List<BigQuery.QueryResultsOption>> buildQueryJobConfig(
      String sql,
      boolean isDryRun,
      @Nullable Map<String, QueryParameterValue> queryParams,
      @Nullable String pageToken,
      @Nullable Integer pageSize,
      @Nullable TableId destinationTable,
      @Nullable Clustering clustering) {
    QueryJobConfiguration.Builder queryJobConfig =
        QueryJobConfiguration.newBuilder(sql)
            .setUseLegacySql(false)
            .setUseQueryCache(true)
            .setDryRun(isDryRun);
    if (queryParams != null) {
      queryParams.forEach((name, val) -> queryJobConfig.addNamedParameter(name, val));
    }
    if (destinationTable != null) {
      queryJobConfig.setDestinationTable(destinationTable);
    }
    if (clustering != null) {
      queryJobConfig.setClustering(clustering);
    }

    List<BigQuery.QueryResultsOption> queryResultsOptions = new ArrayList<>();
    queryResultsOptions.add(
        BigQuery.QueryResultsOption.maxWaitTime(MAX_QUERY_WAIT_TIME.toMillis()));
    if (pageToken != null) {
      queryResultsOptions.add(BigQuery.QueryResultsOption.pageToken(pageToken));
    }
    if (pageSize != null) {
      queryResultsOptions.add(BigQuery.QueryResultsOption.pageSize(pageSize));
    }

    return Pair.of(queryJobConfig.build(), queryResultsOptions);
  }

  // -----------------------------------------------------------------------------------
  // Exceptions and retries

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
    BigQueryException bqEx = (BigQueryException) ex;
    int statusCode = bqEx.getCode();
    LOGGER.error(
        "Caught a BQ error (status code = {}, reason = {}).",
        statusCode,
        bqEx.getError() == null ? null : bqEx.getError().getReason(),
        ex);

    if (isResultSetTooLarge(bqEx)) {
      return false;
    }
    return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT
        // Retry forbidden errors because we often see propagation delays when a user is just
        // granted access.
        || statusCode == HttpStatus.SC_FORBIDDEN
        || bqEx.isRetryable();
  }

  /**
   * Execute a function that includes hitting BQ endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the BQ client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the BQ client or the retries
   */
  private <T> T callWithRetries(
      RetryUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () ->
            RetryUtils.callWithRetries(
                makeRequest,
                GoogleBigQuery::isRetryable,
                BQ_MAXIMUM_RETRIES,
                RetryUtils.DEFAULT_DURATION_SLEEP_FOR_RETRY),
        errorMsg);
  }

  /**
   * Execute a function that includes hitting BQ endpoints. If an exception is thrown by the BQ
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the BQ client or the retries
   */
  private <T> T handleClientExceptions(
      RetryUtils.SupplierWithCheckedException<T, IOException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (BigQueryException bqEx) {
      if (isResultSetTooLarge(bqEx)) {
        throw new InvalidQueryException(
            "Query too large to preview or export to a single file. You can still run this SQL in BigQuery directly, but you'll need to specify a destination table for the result set. See https://cloud.google.com/bigquery/docs/writing-results#large-results and https://cloud.google.com/bigquery/docs/exporting-data for more details.",
            bqEx);
      } else {
        throw bqEx;
      }
    } catch (IOException | InterruptedException ex) {
      // Wrap the BQ exception and re-throw it.
      throw new SystemException(errorMsg, ex);
    }
  }

  private static boolean isResultSetTooLarge(BigQueryException bqEx) {
    final String responseTooLarge = "responseTooLarge";
    boolean isResponseTooLarge =
        bqEx.getCode() == HttpStatus.SC_FORBIDDEN
            && bqEx.getError() != null
            && responseTooLarge.equals(bqEx.getError().getReason());

    final String invalid = "invalid";
    final String tooLargeToExportToSingleFile = "too large to be exported to a single file";
    boolean isTooLargeToExportToSingleFile =
        bqEx.getCode() == 0
            && bqEx.getError() != null
            && invalid.equals(bqEx.getError().getReason())
            && bqEx.getError().getMessage().contains(tooLargeToExportToSingleFile);

    return isResponseTooLarge || isTooLargeToExportToSingleFile;
  }
}
