package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.query.RowResult;

public class SqlQueryResult {
  private final Iterable<RowResult> rowResults;
  private final PageMarker nextPageMarker;
  private final long totalNumRows;

  public SqlQueryResult(
      Iterable<RowResult> rowResults, PageMarker nextPageMarker, long totalNumRows) {
    this.rowResults = rowResults;
    this.nextPageMarker = nextPageMarker;
    this.totalNumRows = totalNumRows;
  }

  public Iterable<RowResult> getRowResults() {
    return rowResults;
  }

  public PageMarker getNextPageMarker() {
    return nextPageMarker;
  }

  public long getTotalNumRows() {
    return totalNumRows;
  }
}
