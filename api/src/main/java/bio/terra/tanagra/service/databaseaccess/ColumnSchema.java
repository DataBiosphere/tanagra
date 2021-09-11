package bio.terra.tanagra.service.databaseaccess;

import bio.terra.tanagra.model.DataType;
import com.google.auto.value.AutoValue;

/** The schema for a column in a {@link RowResult} describing the data in a column. */
@AutoValue
public abstract class ColumnSchema {

  /** The name of the column. */
  public abstract String name();

  /** The type of data in the column. */
  public abstract DataType dataType();

  public static Builder builder() {
    return new AutoValue_ColumnSchema.Builder();
  }

  /** Builder for {@link ColumnSchema}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract Builder dataType(DataType dataType);

    public abstract ColumnSchema build();
  }
}
