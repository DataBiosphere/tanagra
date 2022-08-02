package bio.terra.tanagra.underlay.datapointer;

import bio.terra.tanagra.serialization.datapointer.UFBigQueryDataset;
import bio.terra.tanagra.underlay.DataPointer;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public class BigQueryDataset extends DataPointer {
  private String projectId;
  private String datasetId;

  public BigQueryDataset(String name, String projectId, String datasetId) {
    super(name);
    this.projectId = projectId;
    this.datasetId = datasetId;
  }

  public static BigQueryDataset fromSerialized(UFBigQueryDataset serialized) {
    if (serialized.projectId == null || serialized.projectId.isEmpty()) {
      throw new IllegalArgumentException("No BigQuery project ID defined");
    }
    if (serialized.datasetId == null || serialized.datasetId.isEmpty()) {
      throw new IllegalArgumentException("No BigQuery dataset ID defined");
    }
    return new BigQueryDataset(serialized.name, serialized.projectId, serialized.datasetId);
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

  @Override
  public String getTablePathForIndexing(String tableName) {
    String template = "${projectId}:${datasetId}.${tableName}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("projectId", projectId)
            .put("datasetId", datasetId)
            .put("tableName", tableName)
            .build();
    return StringSubstitutor.replace(template, params);
  }
}
