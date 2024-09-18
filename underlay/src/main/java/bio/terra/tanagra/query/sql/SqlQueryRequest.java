package bio.terra.tanagra.query.sql;

import bio.terra.tanagra.api.query.PageMarker;
import jakarta.annotation.Nullable;
import java.util.Optional;

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

  public String getWhereClause() {
    return Optional.of(sql.indexOf(" WHERE"))
        .filter(index -> index != -1)
        .map(index -> sql.substring(index))
        .orElse("");
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
}
