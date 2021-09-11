package bio.terra.tanagra.service.underlay;

import bio.terra.tanagra.model.DataType;
import com.google.auto.value.AutoValue;

/** A SQL column within a table in an underlay. */
@AutoValue
public abstract class Column {
  /** The name of the column. */
  public abstract String name();

  /** The datatype of the column. */
  public abstract DataType dataType();

  /** The {@link Table} that contains this column. */
  public abstract Table table();

  public static Column create(String name, DataType dataType, Table table) {
    return builder().name(name).dataType(dataType).table(table).build();
  }

  public static Builder builder() {
    return new AutoValue_Column.Builder();
  }

  /** A builder for {@link Column}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String name);

    public abstract Builder dataType(DataType dataType);

    public abstract Builder table(Table table);

    public abstract Column build();
  }
}
