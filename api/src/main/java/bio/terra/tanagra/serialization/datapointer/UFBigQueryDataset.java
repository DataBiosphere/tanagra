package bio.terra.tanagra.serialization.datapointer;

import bio.terra.tanagra.serialization.UFDataPointer;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a data pointer to a BigQuery dataset.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFBigQueryDataset.Builder.class)
public class UFBigQueryDataset extends UFDataPointer {
  private final String projectId;
  private final String datasetId;
  private final String serviceAccountKeyFile;

  public UFBigQueryDataset(BigQueryDataset dataPointer) {
    super(dataPointer);
    this.projectId = dataPointer.getProjectId();
    this.datasetId = dataPointer.getDatasetId();
    this.serviceAccountKeyFile = dataPointer.getServiceAccountKeyFile().toAbsolutePath().toString();
  }

  private UFBigQueryDataset(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.serviceAccountKeyFile = builder.serviceAccountKeyFile;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFDataPointer.Builder {
    private String projectId;
    private String datasetId;
    private String serviceAccountKeyFile;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public Builder serviceAccountKeyFile(String serviceAccountKeyFile) {
      this.serviceAccountKeyFile = serviceAccountKeyFile;
      return this;
    }

    /** Call the private constructor. */
    @Override
    public UFBigQueryDataset build() {
      return new UFBigQueryDataset(this);
    }
  }

  /** Deserialize to the internal representation of the data pointer. */
  @Override
  public BigQueryDataset deserializeToInternal() {
    return BigQueryDataset.fromSerialized(this);
  }

  public String getProjectId() {
    return projectId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public String getServiceAccountKeyFile() {
    return serviceAccountKeyFile;
  }
}
