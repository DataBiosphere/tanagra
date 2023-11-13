package bio.terra.tanagra.query;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.text.StringSubstitutor;

public final class TablePointer implements SQLExpression {
  // TODO: Move these BQ-specific properties and related logic into a sub-class.
  private final String projectId;
  private final String datasetId;
  private final String tableName;
  private final String sql;

  public TablePointer(String projectId, String datasetId, String tableName) {
    this.projectId = projectId;
    this.datasetId = datasetId;
    this.tableName = tableName;
    this.sql = null;
  }

  public TablePointer(String sql) {
    this.projectId = null;
    this.datasetId = null;
    this.tableName = null;
    this.sql = sql;
  }

  public String getTableName() {
    return tableName;
  }

  public boolean isRawSql() {
    return sql != null;
  }

  public String getSql() {
    return sql;
  }

  @Override
  public String renderSQL() {
    if (isRawSql()) {
      return "(" + sql + ")";
    } else {
      String template = "`${projectId}.${datasetId}`.${tableName}";
      Map<String, String> params =
          ImmutableMap.<String, String>builder()
              .put("projectId", projectId)
              .put("datasetId", datasetId)
              .put("tableName", tableName)
              .build();
      return StringSubstitutor.replace(template, params);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TablePointer that = (TablePointer) o;
    return Objects.equals(projectId, that.projectId)
        && Objects.equals(datasetId, that.datasetId)
        && Objects.equals(tableName, that.tableName)
        && Objects.equals(sql, that.sql);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, datasetId, tableName, sql);
  }
}
