package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** A filter on a SQL column within a table in an underlay. */
@AutoValue
public abstract class TableFilter {
  /** A binary filter on a single column. */
  @Nullable
  public abstract BinaryColumnFilter binaryColumnFilter();

  public static TableFilter.Builder builder() {
    return new AutoValue_TableFilter.Builder();
  }

  /** A builder for {@link TableFilter}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder binaryColumnFilter(BinaryColumnFilter columnFilter);

    public abstract TableFilter build();
  }
}
