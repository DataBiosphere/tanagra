package bio.terra.tanagra.underlay;

import com.google.auto.value.AutoValue;

/** An identifier for a {@link Column} in an {@link Underlay}. */
@AutoValue
public abstract class ColumnId {
  /** The name of the {@link BigQueryDataset} for this column. */
  public abstract String dataset();
  /** The name of the {@link Table} for this column. */
  public abstract String table();
  /** The name of the column. */
  public abstract String column();

  public static Builder builder() {
    return new AutoValue_ColumnId.Builder();
  }

  /** A builder for {@link ColumnId}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder dataset(String dataset);

    public abstract Builder table(String table);

    public abstract Builder column(String column);

    public abstract ColumnId build();
  }
}
