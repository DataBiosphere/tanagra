package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;

/** A filter on a SQL column within a table in an underlay. */
@AutoValue
public abstract class BinaryColumnFilter {
  /** The column. */
  public abstract Column column();

  /** The operator to use when comparing the column against the value. */
  public abstract BinaryColumnFilterOperator operator();

  /** The column value. */
  public abstract ColumnValue value();

  public static BinaryColumnFilter create(
      Column column, BinaryColumnFilterOperator operator, ColumnValue value) {
    return builder().column(column).operator(operator).value(value).build();
  }

  public static BinaryColumnFilter.Builder builder() {
    return new AutoValue_BinaryColumnFilter.Builder();
  }

  /** A builder for {@link BinaryColumnFilter}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder column(Column column);

    public abstract Builder operator(BinaryColumnFilterOperator operator);

    public abstract Builder value(ColumnValue value);

    public abstract BinaryColumnFilter build();
  }
}
