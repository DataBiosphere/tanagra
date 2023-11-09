package bio.terra.tanagra.serialization.datapointer;

import bio.terra.tanagra.serialization.UFDataPointer;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a data pointer to a BigQuery dataset.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 */
@JsonDeserialize(builder = UFBigQueryDataset.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UFBigQueryDataset extends UFDataPointer {
  private final String projectId;
  private final String datasetId;
  private final String queryProjectId;
  private final String dataflowServiceAccountEmail;
  private final String dataflowTempLocation;
  private final String dataflowRegion;
  private final String dataflowWorkerMachineType;
  private final boolean dataflowUsePublicIps;
  private final String dataflowSubnetworkName;

  public UFBigQueryDataset(BigQueryDataset dataPointer) {
    super(dataPointer);
    this.projectId = dataPointer.getProjectId();
    this.datasetId = dataPointer.getDatasetId();
    this.queryProjectId = dataPointer.getQueryProjectId();
    this.dataflowServiceAccountEmail = dataPointer.getDataflowServiceAccountEmail();
    this.dataflowTempLocation = dataPointer.getDataflowTempLocation();
    this.dataflowRegion = dataPointer.getDataflowRegion();
    this.dataflowWorkerMachineType = dataPointer.getDataflowWorkerMachineType();
    this.dataflowUsePublicIps = dataPointer.isDataflowUsePublicIps();
    this.dataflowSubnetworkName = dataPointer.getDataflowSubnetworkName();
  }

  private UFBigQueryDataset(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.queryProjectId = builder.queryProjectId;
    this.dataflowServiceAccountEmail = builder.dataflowServiceAccountEmail;
    this.dataflowTempLocation = builder.dataflowTempLocation;
    this.dataflowRegion = builder.dataflowRegion;
    this.dataflowWorkerMachineType = builder.dataflowWorkerMachineType;
    this.dataflowUsePublicIps = builder.dataflowUsePublicIps;
    this.dataflowSubnetworkName = builder.dataflowSubnetworkName;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFDataPointer.Builder {
    private String projectId;
    private String datasetId;
    private String queryProjectId;
    private String dataflowServiceAccountEmail;
    private String dataflowTempLocation;
    private String dataflowRegion;
    private String dataflowWorkerMachineType;
    private boolean dataflowUsePublicIps = true;
    private String dataflowSubnetworkName;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public Builder queryProjectId(String queryProjectId) {
      this.queryProjectId = queryProjectId;
      return this;
    }

    public Builder dataflowServiceAccountEmail(String dataflowServiceAccountEmail) {
      this.dataflowServiceAccountEmail = dataflowServiceAccountEmail;
      return this;
    }

    public Builder dataflowTempLocation(String dataflowTempLocation) {
      this.dataflowTempLocation = dataflowTempLocation;
      return this;
    }

    public Builder dataflowRegion(String dataflowRegion) {
      this.dataflowRegion = dataflowRegion;
      return this;
    }

    public Builder dataflowWorkerMachineType(String dataflowWorkerMachineType) {
      this.dataflowWorkerMachineType = dataflowWorkerMachineType;
      return this;
    }

    public Builder dataflowUsePublicIps(boolean dataflowUsePublicIps) {
      this.dataflowUsePublicIps = dataflowUsePublicIps;
      return this;
    }

    public Builder dataflowSubnetworkName(String dataflowSubnetworkName) {
      this.dataflowSubnetworkName = dataflowSubnetworkName;
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

  public String getQueryProjectId() {
    return queryProjectId;
  }

  public String getDataflowServiceAccountEmail() {
    return dataflowServiceAccountEmail;
  }

  public String getDataflowTempLocation() {
    return dataflowTempLocation;
  }

  public String getDataflowRegion() {
    return dataflowRegion;
  }

  public String getDataflowWorkerMachineType() {
    return dataflowWorkerMachineType;
  }

  public boolean isDataflowUsePublicIps() {
    return dataflowUsePublicIps;
  }

  public String getDataflowSubnetworkName() {
    return dataflowSubnetworkName;
  }
}