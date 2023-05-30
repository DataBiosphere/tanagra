package bio.terra.tanagra.query;

import com.google.common.base.Preconditions;
import java.util.Iterator;

/** The result of a data access query. */
public class QueryResult {
  private final Iterable<RowResult> rowResults;
  private final ColumnHeaderSchema columnHeaderSchema;
  private final PageMarker nextPageMarker;

  public QueryResult(Iterable<RowResult> rowResults, ColumnHeaderSchema columnHeaderSchema) {
    this.rowResults = rowResults;
    this.columnHeaderSchema = columnHeaderSchema;
    this.nextPageMarker = null;
  }

  public QueryResult(
      Iterable<RowResult> rowResults,
      ColumnHeaderSchema columnHeaderSchema,
      PageMarker nextPageMarker) {
    this.rowResults = rowResults;
    this.columnHeaderSchema = columnHeaderSchema;
    this.nextPageMarker = nextPageMarker;
  }

  /** The {@link RowResult}s that make of the data of the query result. */
  public Iterable<RowResult> getRowResults() {
    return rowResults;
  }

  /** The {@link ColumnHeaderSchema}s for the {@link #getRowResults()}. */
  public ColumnHeaderSchema getColumnHeaderSchema() {
    return columnHeaderSchema;
  }

  public PageMarker getNextPageMarker() {
    return nextPageMarker;
  }

  /** Expect a single {@link RowResult} and return it. */
  public RowResult getSingleRowResult() {
    Iterator<RowResult> rowResultIter = getRowResults().iterator();
    Preconditions.checkArgument(rowResultIter.hasNext(), "No row results were returned");
    RowResult rowResult = rowResultIter.next();
    Preconditions.checkArgument(!rowResultIter.hasNext(), "More than one row result was returned");
    return rowResult;
  }
}
