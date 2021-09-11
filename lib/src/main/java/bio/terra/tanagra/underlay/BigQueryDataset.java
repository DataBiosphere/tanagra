package bio.terra.tanagra.underlay;

import com.google.auto.value.AutoValue;

/** A BigQueryDataset. */
@AutoValue
public abstract class BigQueryDataset {
  /** The tanagra name for the dataset. */
  public abstract String name();

  /* The id of the Google project containing the dataset. */
  public abstract String projectId();
  /* The id of the BigQuery dataset. */
  public abstract String datasetId();

  public static Builder builder() {
    return new AutoValue_BigQueryDataset.Builder();
  }

  /** Builder for {@link BigQueryDataset}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String value);

    public abstract Builder projectId(String projectId);

    public abstract Builder datasetId(String datasetId);

    public abstract BigQueryDataset build();
  }
}
