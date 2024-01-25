package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.query.PageMarker;

public class SqlQueryResult {
  private final Iterable<SqlRowResult> rowResults;
  private final PageMarker nextPageMarker;
  private final long totalNumRows;

  private final String sqlNoParams;

  public SqlQueryResult(
      Iterable<SqlRowResult> rowResults,
      PageMarker nextPageMarker,
      long totalNumRows,
      String sqlNoParams) {
    this.rowResults = rowResults;
    this.nextPageMarker = nextPageMarker;
    this.totalNumRows = totalNumRows;
    this.sqlNoParams = sqlNoParams;
  }

  public Iterable<SqlRowResult> getRowResults() {
    return rowResults;
  }

  public PageMarker getNextPageMarker() {
    return nextPageMarker;
  }

  public long getTotalNumRows() {
    return totalNumRows;
  }

  public String getSqlNoParams() {
    return sqlNoParams;
  }
}
