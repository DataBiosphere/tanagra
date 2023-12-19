package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.query.PageMarker;

public class SqlQueryRequest {
  private final String sql;
  private final SqlParams sqlParams;
  private final PageMarker pageMarker;
  private final Integer pageSize;

  public SqlQueryRequest(String sql, SqlParams sqlParams, PageMarker pageMarker, Integer pageSize) {
    this.sql = sql;
    this.sqlParams = sqlParams;
    this.pageMarker = pageMarker;
    this.pageSize = pageSize;
  }

  public String getSql() {
    return sql;
  }

  public SqlParams getSqlParams() {
    return sqlParams;
  }

  public PageMarker getPageMarker() {
    return pageMarker;
  }

  public Integer getPageSize() {
    return pageSize;
  }
}
