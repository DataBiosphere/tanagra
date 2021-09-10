package bio.terra.tanagra.service.databaseaccess.bigquery;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.tanagra.service.databaseaccess.QueryExecutor;
import bio.terra.tanagra.service.databaseaccess.QueryRequest;
import bio.terra.tanagra.service.databaseaccess.QueryResult;
import bio.terra.tanagra.service.databaseaccess.RowResult;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQuery.QueryResultsOption;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.Iterables;
import java.time.Duration;
import javax.servlet.http.HttpServletResponse;

/** A {@link QueryExecutor} for Google BigQuery. */
public class BigQueryExecutor implements QueryExecutor {
  private static final Duration MAX_QUERY_WAIT_TIME = Duration.ofSeconds(60);

  /** The BigQuery client to use for executing queries. */
  private final BigQuery bigQuery;

  public BigQueryExecutor(BigQuery bigQuery) {
    this.bigQuery = bigQuery;
  }

  @Override
  public QueryResult execute(QueryRequest queryRequest) {
    QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryRequest.sql()).build();
    // TODO add pagination. Right now we always only return the first page of results.
    TableResult tableResult = getResults(queryJobConfiguration);

    Iterable<RowResult> rowResults =
        Iterables.transform(
            tableResult.getValues(),
            (FieldValueList fieldValueList) ->
                new BigQueryRowResult(fieldValueList, queryRequest.columnHeaderSchema()));

    return QueryResult.builder()
        .columnHeaderSchema(queryRequest.columnHeaderSchema())
        .rowResults(rowResults)
        .build();
  }

  private TableResult getResults(QueryJobConfiguration queryJobConfiguration) {
    Job job = bigQuery.create(JobInfo.newBuilder(queryJobConfiguration).build());
    try {
      return job.getQueryResults(QueryResultsOption.maxWaitTime(MAX_QUERY_WAIT_TIME.toMillis()));
    } catch (InterruptedException e) {
      throw new InternalServerErrorException("Timed out waiting for BigQuery results.", e);
    } catch (BigQueryException e) {
      if (e.getCode() == HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
        throw new InternalServerErrorException(
            "BigQuery was temporarily unavailable, try again later", e);
      } else {
        throw new InternalServerErrorException(
            String.format(
                "An unexpected error occurred querying against BigQuery with query (%s)",
                queryJobConfiguration.getQuery()),
            e);
      }
    }
  }
}
