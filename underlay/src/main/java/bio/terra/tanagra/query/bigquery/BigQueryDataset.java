package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.query.DataPointer;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public final class BigQueryDataset extends DataPointer {
  private final String projectId;
  private final String datasetId;

  @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
  public BigQueryDataset(String name, String projectId, String datasetId) {
    super(name);
    this.projectId = projectId;
    this.datasetId = datasetId;
  }

  @Override
  public String getTableSQL(String tableName) {
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
