package bio.terra.tanagra.query2.bigquery;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.query2.sql.SqlQueryRequest;
import bio.terra.tanagra.query2.sql.SqlQueryResult;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BQQueryExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(BQQueryExecutor.class);

  private final String queryProjectId;
  private final String datasetLocation;

  private GoogleBigQuery bigQueryService;

  public BQQueryExecutor(String queryProjectId, String datasetLocation) {
    this.queryProjectId = queryProjectId;
    this.datasetLocation = datasetLocation;
  }

  public SqlQueryResult run(SqlQueryRequest queryRequest) {
    // Build the query configuration, including any query parameters.
    QueryJobConfiguration.Builder queryConfig =
        QueryJobConfiguration.newBuilder(queryRequest.getSql()).setUseLegacySql(false);
    queryRequest.getSqlParams().getParams().entrySet().stream()
        .forEach(
            sqlParam ->
                queryConfig.addNamedParameter(
                    sqlParam.getKey(), toQueryParameterValue(sqlParam.getValue())));

    LOGGER.info("Running SQL against BigQuery: {}", queryRequest.getSql());
    TableResult tableResult =
        getBigQueryService()
            .queryBigQuery(
                queryConfig.build(),
                queryRequest.getPageMarker() == null
                    ? null
                    : queryRequest.getPageMarker().getPageToken(),
                queryRequest.getPageSize());

    LOGGER.info("SQL query returns {} rows across all pages", tableResult.getTotalRows());
    //        Iterable<RowResult> rowResults =
    //                Iterables.transform(
    //                        tableResult.getValues() /* Single page of results. */,
    //                        (FieldValueList fieldValueList) ->
    //                                new BigQueryRowResult(fieldValueList,
    // queryRequest.getColumnHeaderSchema()));
    PageMarker nextPageMarker =
        tableResult.hasNextPage() ? PageMarker.forToken(tableResult.getNextPageToken()) : null;

    //        return new SqlQueryResult(
    //                rowResults,
    //                queryRequest.getColumnHeaderSchema(),
    //                nextPageMarker,
    //                tableResult.getTotalRows());
    return null;
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
        return QueryParameterValue.date(literal.getDateValAsString());
      case DOUBLE:
        return QueryParameterValue.float64(literal.getDoubleVal());
      case TIMESTAMP:
        return QueryParameterValue.timestamp(literal.getTimestampValAsString());
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
}
