package bio.terra.tanagra.serialization.datapointer;

import bio.terra.tanagra.serialization.UFDataPointer;
import bio.terra.tanagra.underlay.DataPointer;
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
  public final String projectId;
  public final String datasetId;

  /** Constructor for Jackson deserialization during testing. */
  private UFBigQueryDataset(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFDataPointer.Builder {
    private String projectId;
    private String datasetId;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    /** Call the private constructor. */
    public UFBigQueryDataset build() {
      return new UFBigQueryDataset(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }

  /** Deserialize to the internal representation of the data pointer. */
  public DataPointer deserializeToInternal() {
    return BigQueryDataset.fromSerialized(this);
  }
}
