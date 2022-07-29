package bio.terra.tanagra.underlay.datapointer;

import bio.terra.tanagra.serialization.datapointer.UFBigQueryDataset;
import bio.terra.tanagra.underlay.DataPointer;

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
}
