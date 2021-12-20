package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;

/** A filter on a SQL column within a table in an underlay. */
@AutoValue
public abstract class ColumnFilter {
  /** The column to filter on. */
  public abstract Column column();

  /** The column value to filter on. */
  public abstract ColumnValue value();

  public static ColumnFilter create(Column column, ColumnValue value) {
    return builder().column(column).value(value).build();
  }

  public static ColumnFilter.Builder builder() {
    return new AutoValue_ColumnFilter.Builder();
  }

  /** A builder for {@link ColumnFilter}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder column(Column column);

    public abstract Builder value(ColumnValue value);

    public abstract ColumnFilter build();
  }
}
