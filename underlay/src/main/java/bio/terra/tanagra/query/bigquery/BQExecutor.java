package bio.terra.tanagra.query.bigquery;

import static bio.terra.tanagra.utils.NameUtils.simplifyStringForName;

import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.query.sql.SqlQueryResult;
import bio.terra.tanagra.query.sql.SqlRowResult;
import bio.terra.tanagra.utils.GoogleBigQuery;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.storage.BlobId;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BQExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(BQExecutor.class);
  private static final String TEMPORARY_TABLE_BASE_NAME = "export";
  private final BQExecutorInfrastructure queryInfrastructure;
  private GoogleBigQuery bigQueryService;
  private GoogleCloudStorage cloudStorageService;

  public BQExecutor(BQExecutorInfrastructure queryInfrastructure) {
    this.queryInfrastructure = queryInfrastructure;
  }

  public SqlQueryResult run(SqlQueryRequest queryRequest) {
    // Log the SQL statement with parameters substituted locally (i.e. not by BQ) for debugging.
    String sqlNoParams = queryRequest.sql();
    for (String paramName : queryRequest.sqlParams().getParamNamesLongestFirst()) {
      sqlNoParams =
          sqlNoParams.replaceAll(
              '@' + paramName,
              toSql(toQueryParameterValue(queryRequest.sqlParams().getParamValue(paramName))));
    }

    // Build a BQ parameter value object for each SQL query parameter.
    Map<String, QueryParameterValue> bqQueryParams = toQueryParameterMap(queryRequest.sqlParams());

    // Pull the page token from the request, if we're paging.
    String pageToken =
        queryRequest.pageMarker() == null ? null : queryRequest.pageMarker().getPageToken();

    if (queryRequest.isDryRun()) {
      // For a dry run, validate the query and log some statistics.
      getBigQueryService()
          .dryRunQuery(
              queryRequest.sql(), bqQueryParams, pageToken, queryRequest.pageSize(), null, null);
      return new SqlQueryResult(List.of(), null, 0, sqlNoParams);
    } else {
      // For a regular run, convert the BQ query results to our internal result objects.
      TableResult tableResult =
          getBigQueryService()
              .runQuery(
                  queryRequest.sql(),
                  bqQueryParams,
                  pageToken,
                  queryRequest.pageSize(),
                  null,
                  null,
                  null);
      Iterable<SqlRowResult> rowResults =
          Iterables.transform(
              tableResult.getValues() /* Single page of results. */, BQRowResult::new);
      PageMarker nextPageMarker =
          tableResult.hasNextPage() ? PageMarker.forToken(tableResult.getNextPageToken()) : null;
      return new SqlQueryResult(
          rowResults, nextPageMarker, tableResult.getTotalRows(), sqlNoParams);
    }
  }

  /**
   * @return pair of strings: GCS URL, file name
   */
  public Pair<String, String> exportQuery(
      SqlQueryRequest queryRequest, String fileNamePrefix, boolean generateSignedUrl) {
    LOGGER.info("Exporting BQ query: {}", queryRequest.sql());

    // Create a temporary table with the results of the query.
    final int bqMaxTableNameLength = 1024;
    String tempTableName =
        TEMPORARY_TABLE_BASE_NAME
            + "_"
            + simplifyStringForName(UUID.randomUUID().toString())
            + "_"
            + simplifyStringForName(fileNamePrefix);
    if (tempTableName.length() > bqMaxTableNameLength) {
      tempTableName = tempTableName.substring(0, bqMaxTableNameLength);
    }
    String exportDatasetId =
        getBigQueryService()
            .findDatasetWithLocation(
                queryInfrastructure.getQueryProjectId(),
                queryInfrastructure.getExportDatasetIds(),
                queryInfrastructure.getDatasetLocation());
    TableId tempTableId =
        TableId.of(queryInfrastructure.getQueryProjectId(), exportDatasetId, tempTableName);

    // Build a BQ parameter value object for each SQL query parameter.
    Map<String, QueryParameterValue> bqQueryParams = toQueryParameterMap(queryRequest.sqlParams());
    getBigQueryService()
        .runQuery(queryRequest.sql(), bqQueryParams, null, null, tempTableId, null, null);
    Table tempTable =
        getBigQueryService()
            .pollForTableExistenceOrThrow(
                queryInfrastructure.getQueryProjectId(),
                exportDatasetId,
                tempTableName,
                10,
                Duration.ofSeconds(1));
    LOGGER.info(
        "Temporary table created for export: {}.{}.{}",
        queryInfrastructure.getQueryProjectId(),
        exportDatasetId,
        tempTableName);
    if (BigInteger.ZERO.equals(tempTable.getNumRows())) {
      LOGGER.info(
          "Temporary table has no rows, skipping export: {}.{}.{}",
          queryInfrastructure.getQueryProjectId(),
          exportDatasetId,
          tempTableName);
      return Pair.of(null, null);
    }

    // Export the temporary table to a compressed file.
    String bucketName =
        getCloudStorageService()
            .findBucketForBigQueryExport(
                queryInfrastructure.getQueryProjectId(),
                queryInfrastructure.getExportBucketNames(),
                queryInfrastructure.getDatasetLocation());
    String fileName = fileNamePrefix + ".csv.gzip";
    String gcsUrl = String.format("gs://%s/%s", bucketName, fileName);
    LOGGER.info("Exporting temporary table to GCS file: {}", gcsUrl);
    Job exportJob = getBigQueryService().exportTableToGcs(tempTableId, gcsUrl, "GZIP", "CSV");
    if (exportJob == null) {
      throw new SystemException("BigQuery extract job failed: job no longer exists");
    } else if (exportJob.getStatus().getError() != null) {
      throw new SystemException("BigQuery extract job failed: " + exportJob.getStatus().getError());
    }
    LOGGER.info("Export of temporary table completed: {}", exportJob.getStatus().getState());

    if (!generateSignedUrl) {
      String gcsUrlHttps = String.format("https://storage.cloud.google.com/%s/%s", bucketName, fileName);
      return Pair.of(gcsUrlHttps, fileName);
    }

    // Generate a signed URL to the file.
    return Pair.of(getCloudStorageService().createSignedUrl(gcsUrl), fileName);
  }

  /**
   * @return pair of strings: GCS URL, file name
   */
  public Pair<String, String> exportRawData(
      String fileContents, String fileName, boolean generateSignedUrl) {
    LOGGER.info("Exporting raw data: {}", fileName);

    // Just pick the first GCS bucket name.
    String bucketName = queryInfrastructure.getExportBucketNames().get(0);

    // Write the file.
    BlobId blobId = getCloudStorageService().writeFile(bucketName, fileName, fileContents);
    String gcsUrl = blobId.toGsUtilUri();
    LOGGER.info("Exported raw data to GCS: {}", gcsUrl);

    if (!generateSignedUrl) {
      return Pair.of(gcsUrl, fileName);
    }

    // Generate a signed URL to the file.
    return Pair.of(getCloudStorageService().createSignedUrl(gcsUrl), fileName);
  }

  public static String replaceFunctionsThatPreventCaching(
      String sql, SqlParams sqlParams, Instant queryInstant) {
    String modifiedSql = sql;

    final String currentTimestamp = "CURRENT_TIMESTAMP";
    final String currentTimestampParens = currentTimestamp + "()";
    if (sql.contains(currentTimestamp)) {
      String paramName =
          sqlParams.addParam(
              "currentTimestamp", Literal.forTimestamp(Timestamp.from(queryInstant)));
      modifiedSql =
          sql.replace(currentTimestampParens, '@' + paramName)
              .replace(currentTimestamp, '@' + paramName);
    }

    final String currentDate = "CURRENT_DATE";
    final String currentDateParens = currentDate + "()";
    if (sql.contains(currentDate)) {
      String paramName =
          sqlParams.addParam(
              "currentDate",
              Literal.forDate(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(queryInstant)));
      modifiedSql =
          sql.replace(currentDateParens, '@' + paramName).replace(currentDate, '@' + paramName);
    }
    return modifiedSql;
  }

  private static Map<String, QueryParameterValue> toQueryParameterMap(SqlParams sqlParams) {
    // Build a BQ parameter value object for each SQL query parameter.
    Map<String, QueryParameterValue> bqQueryParams = new HashMap<>();
    sqlParams
        .getParams()
        .forEach((key, value) -> bqQueryParams.put(key, toQueryParameterValue(value)));
    return bqQueryParams;
  }

  private static QueryParameterValue toQueryParameterValue(Literal literal) {
    return switch (literal.getDataType()) {
      case INT64 -> QueryParameterValue.int64(literal.getInt64Val());
      case STRING -> QueryParameterValue.string(literal.getStringVal());
      case BOOLEAN -> QueryParameterValue.bool(literal.getBooleanVal());
      case DATE ->
          QueryParameterValue.date(
              literal.getDateVal() == null ? null : literal.getDateVal().toString());
      case DOUBLE -> QueryParameterValue.float64(literal.getDoubleVal());
      case TIMESTAMP ->
          QueryParameterValue.timestamp(
              literal.getTimestampVal() == null
                  ? null
                  : DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss.SSS")
                      .format(literal.getTimestampVal().toLocalDateTime()));
      default ->
          throw new SystemException("Unsupported data type for BigQuery: " + literal.getDataType());
    };
  }

  private static String toSql(QueryParameterValue queryParameterValue) {
    return switch (queryParameterValue.getType()) {
      case INT64, BOOL, FLOAT64 ->
          queryParameterValue.getValue() == null ? "null" : queryParameterValue.getValue();
      case STRING -> "'" + queryParameterValue.getValue() + "'";
      case DATE -> "DATE('" + queryParameterValue.getValue() + "')";
      case TIMESTAMP -> "TIMESTAMP('" + queryParameterValue.getValue() + "')";
      default ->
          throw new SystemException(
              "Unsupported data type for BigQuery: " + queryParameterValue.getType());
    };
  }

  private GoogleBigQuery getBigQueryService() {
    // Lazy load the BigQuery service.
    if (bigQueryService == null) {
      bigQueryService =
          GoogleBigQuery.forApplicationDefaultCredentials(queryInfrastructure.getQueryProjectId());
    }
    return bigQueryService;
  }

  private GoogleCloudStorage getCloudStorageService() {
    // Lazy load the GCS service.
    if (cloudStorageService == null) {
      cloudStorageService =
          GoogleCloudStorage.forApplicationDefaultCredentials(
              queryInfrastructure.getQueryProjectId());
    }
    return cloudStorageService;
  }
}
