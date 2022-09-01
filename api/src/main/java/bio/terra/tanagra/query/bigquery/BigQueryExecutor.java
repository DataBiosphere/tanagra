package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.Iterables;

/** A {@link QueryExecutor} for Google BigQuery. */
public class BigQueryExecutor implements QueryExecutor {
  /** The BigQuery client to use for executing queries. */
  private final GoogleBigQuery bigQuery;

  public BigQueryExecutor(GoogleBigQuery bigQuery) {
    this.bigQuery = bigQuery;
  }

  @Override
  public QueryResult execute(QueryRequest queryRequest) {
    TableResult tableResult = bigQuery.queryBigQuery(queryRequest.getSql());

    Iterable<RowResult> rowResults =
        Iterables.transform(
            tableResult.getValues(),
            (FieldValueList fieldValueList) ->
                new BigQueryRowResult(fieldValueList, queryRequest.getColumnHeaderSchema()));

    return new QueryResult(rowResults, queryRequest.getColumnHeaderSchema());
  }
}
