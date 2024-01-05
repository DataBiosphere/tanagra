package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.query.PageMarker;
import javax.annotation.Nullable;

public class SqlQueryRequest {
  private final String sql;
  private final SqlParams sqlParams;
  private final PageMarker pageMarker;
  private final Integer pageSize;
  private final boolean isDryRun;

  public SqlQueryRequest(
      String sql,
      SqlParams sqlParams,
      @Nullable PageMarker pageMarker,
      @Nullable Integer pageSize,
      boolean isDryRun) {
    this.sql = sql;
    this.sqlParams = sqlParams;
    this.pageMarker = pageMarker;
    this.pageSize = pageSize;
    this.isDryRun = isDryRun;
  }

  public String getSql() {
    return sql;
  }

  public SqlParams getSqlParams() {
    return sqlParams;
  }

  public @Nullable PageMarker getPageMarker() {
    return pageMarker;
  }

  public @Nullable Integer getPageSize() {
    return pageSize;
  }

  public boolean isDryRun() {
    return isDryRun;
  }

  public SqlQueryRequest cloneAndSetSql(String newSql) {
    return new SqlQueryRequest(newSql, sqlParams, pageMarker, pageSize, isDryRun);
  }
}
