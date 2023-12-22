package bio.terra.tanagra.query.bigquery;

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
import com.google.cloud.bigquery.JobStatistics;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BQExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(BQExecutor.class);

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
      return new SqlQueryResult(List.of(), null, 0);
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
      return new SqlQueryResult(rowResults, nextPageMarker, tableResult.getTotalRows());
    }
  }

  public String export(
      SqlQueryRequest queryRequest,
      String fileNamePrefix,
      String exportProjectId,
      List<String> exportBucketNames) {
    // Validate the filename.
    // GCS file name must be in wildcard format.
    // https://cloud.google.com/bigquery/docs/reference/standard-sql/other-statements#export_option_list:~:text=The%20uri%20option%20must%20be%20a%20single%2Dwildcard%20URI
    String validatedFilename;
    if (fileNamePrefix == null || !fileNamePrefix.contains("*")) {
      validatedFilename = "tanagra_" + System.currentTimeMillis() + "_*.csv";
      LOGGER.warn(
          "No (or invalid) filename specified for GCS export ({}), using default: {}",
          fileNamePrefix,
          validatedFilename);
    } else {
      validatedFilename = fileNamePrefix;
    }

    // Prefix the SQL query with the export to GCS directive.
    String bucketName =
        getCloudStorageService()
            .findBucketForBigQueryExport(exportProjectId, exportBucketNames, datasetLocation);
    String sqlWithExportPrefix =
        String.format(
            "EXPORT DATA OPTIONS(uri='gs://%s/%s',format='CSV',overwrite=true,header=true) AS %n%s",
            bucketName, validatedFilename, queryRequest.getSql());
    run(queryRequest.cloneAndSetSql(sqlWithExportPrefix));

    // Multiple files will be created only if export is very large (> 1GB). For now, just assume
    // only "000000000000" was created.
    // TODO: Detect and handle case where mulitple files are created.
    return String.format("gs://%s/%s", bucketName, validatedFilename.replace("*", "000000000000"));
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
