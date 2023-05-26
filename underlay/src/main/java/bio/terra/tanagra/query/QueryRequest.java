package bio.terra.tanagra.query;

/** The request for a query to execute against a database backend. */
public class QueryRequest {
  // TODO: add parametrized arguments in SQL string
  private final String sql;
  private final ColumnHeaderSchema columnHeaderSchema;
  private final PageMarker pageMarker;
  private final Integer pageSize;

  public QueryRequest(String sql, ColumnHeaderSchema columnHeaderSchema) {
    this.sql = sql;
    this.columnHeaderSchema = columnHeaderSchema;
    this.pageMarker = null;
    this.pageSize = null;
  }

  public QueryRequest(
      String sql, ColumnHeaderSchema columnHeaderSchema, PageMarker pageMarker, Integer pageSize) {
    this.sql = sql;
    this.columnHeaderSchema = columnHeaderSchema;
    this.pageMarker = pageMarker;
    this.pageSize = pageSize;
  }

  public String getSql() {
    return sql;
  }

  public ColumnHeaderSchema getColumnHeaderSchema() {
    return columnHeaderSchema;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }

  public Integer getPageSize() {
    return pageSize;
  }
}
