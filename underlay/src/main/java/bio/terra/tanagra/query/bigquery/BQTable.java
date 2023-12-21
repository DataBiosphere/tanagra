package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.query.sql.SqlTable;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.text.StringSubstitutor;

public class BQTable extends SqlTable {
  private final String projectId;
  private final String datasetId;

  public BQTable(String projectId, String datasetId, String tableName) {
    super(tableName, null);
    this.projectId = projectId;
    this.datasetId = datasetId;
  }

  public BQTable(String sql) {
    super(null, sql);
    this.projectId = null;
    this.datasetId = null;
  }

  @Override
  public String renderForQuery() {
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
    BQTable that = (BQTable) o;
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
