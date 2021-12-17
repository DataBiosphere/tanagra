package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;

/** A filter on a SQL column within a table in an underlay. */
@AutoValue
public abstract class TableFilter {
  /** The filter on a specific column. */
  public abstract ColumnFilter columnFilter();

  public static TableFilter create(ColumnFilter columnFilter) {
    return builder().columnFilter(columnFilter).build();
  }

  public static TableFilter.Builder builder() {
    return new AutoValue_TableFilter.Builder();
  }

  /** A builder for {@link TableFilter}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder columnFilter(ColumnFilter columnFilter);

    public abstract TableFilter build();
  }
}
