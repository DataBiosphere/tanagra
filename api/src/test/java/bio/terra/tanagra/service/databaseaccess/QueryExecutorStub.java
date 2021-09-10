package bio.terra.tanagra.service.databaseaccess;

/** A stub {@link QueryExecutor} for testing. */
public class QueryExecutorStub implements QueryExecutor {
  /** The query result to return for {@link #execute(QueryRequest)}. */
  private QueryResult queryResult;
  /** The most recent QueryRequest seen in {@link #execute(QueryRequest)}. */
  private QueryRequest latestQueryRequest;

  @Override
  public QueryResult execute(QueryRequest queryRequest) {
    latestQueryRequest = queryRequest;
    return queryResult;
  }

  public void setQueryResult(QueryResult queryResult) {
    this.queryResult = queryResult;
  }

  public QueryRequest getLatestQueryRequest() {
    return latestQueryRequest;
  }
}
