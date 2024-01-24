package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.query.PageMarker;

public class SqlQueryResult {
  private final Iterable<SqlRowResult> rowResults;
  private final PageMarker nextPageMarker;
  private final long totalNumRows;

  private final String nonParameterizedSql;

  public SqlQueryResult(
      Iterable<SqlRowResult> rowResults,
      PageMarker nextPageMarker,
      long totalNumRows,
      String nonParameterizedSql) {
    this.rowResults = rowResults;
    this.nextPageMarker = nextPageMarker;
    this.totalNumRows = totalNumRows;
    this.nonParameterizedSql = nonParameterizedSql;
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

  public String getNonParameterizedSql() {
    return nonParameterizedSql;
  }
}
