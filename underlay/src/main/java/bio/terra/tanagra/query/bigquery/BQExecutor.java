package bio.terra.tanagra.query.bigquery;

import static bio.terra.tanagra.utils.NameUtils.simplifyStringForName;

import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.sql.SqlQueryRequest;
import bio.terra.tanagra.query.sql.SqlQueryResult;
import bio.terra.tanagra.query.sql.SqlRowResult;
import bio.terra.tanagra.utils.GoogleBigQuery;
import bio.terra.tanagra.utils.GoogleCloudStorage;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobStatistics;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BQExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(BQExecutor.class);
  private static final String TEMPORARY_TABLE_BASE_NAME = "export";

  private final String queryProjectId;
  private final String datasetLocation;

  private GoogleBigQuery bigQueryService;
  private GoogleCloudStorage cloudStorageService;

  public BQExecutor(String queryProjectId, String datasetLocation) {
    this.queryProjectId = queryProjectId;
    this.datasetLocation = datasetLocation;
  }

  public SqlQueryResult run(SqlQueryRequest queryRequest) {
    QueryJobConfiguration.Builder queryConfig =
        QueryJobConfiguration.newBuilder(queryRequest.getSql()).setUseLegacySql(false);
    queryRequest.getSqlParams().getParams().entrySet().stream()
        .forEach(
            sqlParam ->
                queryConfig.addNamedParameter(
                    sqlParam.getKey(), toQueryParameterValue(sqlParam.getValue())));
    queryConfig.setDryRun(queryRequest.isDryRun());
    queryConfig.setUseQueryCache(true);

    String sqlNoParams = queryRequest.getSql();
    for (String paramName : queryRequest.getSqlParams().getParamNamesLongestFirst()) {
      sqlNoParams =
          sqlNoParams.replaceAll(
              '@' + paramName,
              toSql(toQueryParameterValue(queryRequest.getSqlParams().getParamValue(paramName))));
    }
    LOGGER.info("SQL no parameters: {}", sqlNoParams);

    LOGGER.info("Running SQL against BigQuery: {}", queryRequest.getSql());
    if (queryRequest.isDryRun()) {
      JobStatistics.QueryStatistics queryStatistics =
          getBigQueryService().queryStatistics(queryConfig.build());
      LOGGER.info(
          "SQL dry run: statementType={}, cacheHit={}, totalBytesProcessed={}, totalSlotMs={}",
          queryStatistics.getStatementType(),
          queryStatistics.getCacheHit(),
          queryStatistics.getTotalBytesProcessed(),
          queryStatistics.getTotalSlotMs());
      return new SqlQueryResult(List.of(), null, 0, sqlNoParams);
    } else {
      TableResult tableResult =
          getBigQueryService()
              .queryBigQuery(
                  queryConfig.build(),
                  queryRequest.getPageMarker() == null
                      ? null
                      : queryRequest.getPageMarker().getPageToken(),
                  queryRequest.getPageSize());

      LOGGER.info("SQL query returns {} rows across all pages", tableResult.getTotalRows());
      Iterable<SqlRowResult> rowResults =
          Iterables.transform(
              tableResult.getValues() /* Single page of results. */,
              (FieldValueList fieldValueList) -> new BQRowResult(fieldValueList));
      PageMarker nextPageMarker =
          tableResult.hasNextPage() ? PageMarker.forToken(tableResult.getNextPageToken()) : null;
      return new SqlQueryResult(
          rowResults, nextPageMarker, tableResult.getTotalRows(), sqlNoParams);
    }
  }

  public String export(
      SqlQueryRequest queryRequest,
      String fileNamePrefix,
      String exportProjectId,
      List<String> exportDatasetIds,
      List<String> exportBucketNames,
      boolean generateSignedUrl) {
    LOGGER.info("Exporting BQ query: {}", queryRequest.getSql());

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
            .findDatasetForExportTempTable(exportProjectId, exportDatasetIds, datasetLocation);
    TableId tempTableId = TableId.of(exportProjectId, exportDatasetId, tempTableName);

    // Pass query parameters to the job.
    QueryJobConfiguration.Builder queryConfig =
        QueryJobConfiguration.newBuilder(queryRequest.getSql())
            .setUseLegacySql(false)
            .setDestinationTable(tempTableId);
    queryRequest.getSqlParams().getParams().entrySet().stream()
        .forEach(
            sqlParam ->
                queryConfig.addNamedParameter(
                    sqlParam.getKey(), toQueryParameterValue(sqlParam.getValue())));
    Table tempTable = getBigQueryService().createTableFromQuery(queryConfig.build());
    LOGGER.info(
        "Temporary table created for export: {}.{}.{}",
        exportProjectId,
        exportDatasetId,
        tempTableName);
    if (BigInteger.ZERO.equals(tempTable.getNumRows())) {
      LOGGER.info(
          "Temporary table has no rows, skipping export: {}.{}.{}",
          exportProjectId,
          exportDatasetId,
          tempTableName);
      return null;
    }

    // Export the temporary table to a compressed file.
    String bucketName =
        getCloudStorageService()
            .findBucketForBigQueryExport(exportProjectId, exportBucketNames, datasetLocation);
    String gcsUrl = String.format("gs://%s/%s.csv.gzip", bucketName, fileNamePrefix);
    LOGGER.info("Exporting temporary table to GCS file: {}", gcsUrl);
    Job exportJob = getBigQueryService().exportTableToGcs(tempTableId, gcsUrl, "GZIP", "CSV");
    if (exportJob == null) {
      throw new SystemException("BigQuery extract job failed: job no longer exists");
    } else if (exportJob.getStatus().getError() != null) {
      throw new SystemException("BigQuery extract job failed: " + exportJob.getStatus().getError());
    }
    LOGGER.info("Export of temporary table completed: {}", exportJob.getStatus().getState());

    if (!generateSignedUrl) {
      return gcsUrl;
    }

    // Generate a signed URL to the file.
    return getCloudStorageService().createSignedUrl(gcsUrl);
  }

  private static QueryParameterValue toQueryParameterValue(Literal literal) {
    switch (literal.getDataType()) {
      case INT64:
        return QueryParameterValue.int64(literal.getInt64Val());
      case STRING:
        return QueryParameterValue.string(literal.getStringVal());
      case BOOLEAN:
        return QueryParameterValue.bool(literal.getBooleanVal());
      case DATE:
        return QueryParameterValue.date(
            literal.getDateVal() == null ? null : literal.getDateVal().toString());
      case DOUBLE:
        return QueryParameterValue.float64(literal.getDoubleVal());
      case TIMESTAMP:
        return QueryParameterValue.timestamp(
            literal.getTimestampVal() == null ? null : literal.getTimestampVal().toString());
      default:
        throw new SystemException("Unsupported data type for BigQuery: " + literal.getDataType());
    }
  }

  private static String toSql(QueryParameterValue queryParameterValue) {
    switch (queryParameterValue.getType()) {
      case INT64:
      case BOOL:
      case FLOAT64:
        return queryParameterValue.getValue() == null ? "null" : queryParameterValue.getValue();
      case STRING:
        return "'" + queryParameterValue.getValue() + "'";
      case DATE:
        return "DATE('" + queryParameterValue.getValue() + "')";
      case TIMESTAMP:
        return "TIMESTAMP('" + queryParameterValue.getValue() + "')";
      default:
        throw new SystemException(
            "Unsupported data type for BigQuery: " + queryParameterValue.getType());
    }
  }

  private GoogleBigQuery getBigQueryService() {
    // Lazy load the BigQuery service.
    if (bigQueryService == null) {
      GoogleCredentials credentials;
      try {
        credentials = GoogleCredentials.getApplicationDefault();
      } catch (IOException ioEx) {
        throw new SystemException("Error loading application default credentials", ioEx);
      }
      bigQueryService = new GoogleBigQuery(credentials, queryProjectId);
    }
    return bigQueryService;
  }

  private GoogleCloudStorage getCloudStorageService() {
    // Lazy load the GCS service.
    if (cloudStorageService == null) {
      GoogleCredentials credentials;
      try {
        credentials = GoogleCredentials.getApplicationDefault();
      } catch (IOException ioEx) {
        throw new SystemException("Error loading application default credentials", ioEx);
      }
      cloudStorageService = new GoogleCloudStorage(credentials, queryProjectId);
    }
    return cloudStorageService;
  }
}
