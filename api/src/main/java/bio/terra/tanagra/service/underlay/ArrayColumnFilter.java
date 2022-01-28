package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;
import java.util.List;

/**
 * A filter on a table in an underlay that is composed of multiple sub-filters on the table's
 * columns.
 */
@AutoValue
public abstract class ArrayColumnFilter {
  /** The operator to use when composing the sub-filters. */
  public abstract ArrayColumnFilterOperator operator();

  /** The array column sub-filters. */
  public abstract List<ArrayColumnFilter> arrayColumnFilters();

  /** The binary column sub-filters. */
  public abstract List<BinaryColumnFilter> binaryColumnFilters();

  public static ArrayColumnFilter create(
      ArrayColumnFilterOperator operator,
      List<ArrayColumnFilter> arrayColumnFilters,
      List<BinaryColumnFilter> binaryColumnFilters) {
    return builder()
        .operator(operator)
        .arrayColumnFilters(arrayColumnFilters)
        .binaryColumnFilters(binaryColumnFilters)
        .build();
  }

  public static ArrayColumnFilter.Builder builder() {
    return new AutoValue_ArrayColumnFilter.Builder();
  }

  /** A builder for {@link ArrayColumnFilter}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder operator(ArrayColumnFilterOperator value);

    public abstract Builder arrayColumnFilters(List<ArrayColumnFilter> value);

    public abstract Builder binaryColumnFilters(List<BinaryColumnFilter> value);

    public abstract ArrayColumnFilter build();
  }
}
