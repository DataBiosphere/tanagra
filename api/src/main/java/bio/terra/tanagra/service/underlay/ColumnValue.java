package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** A value to filter a SQL column within a table in an underlay. */
@AutoValue
public abstract class ColumnValue {
  @Nullable
  public abstract Long longVal();

  @Nullable
  public abstract String stringVal();

  @Nullable
  public abstract Boolean booleanVal();

  public static ColumnValue.Builder builder() {
    return new AutoValue_ColumnValue.Builder();
  }

  /** A builder for {@link ColumnValue}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder longVal(Long longVal);

    public abstract Builder stringVal(String stringVal);

    public abstract Builder booleanVal(Boolean booleanVal);

    public abstract ColumnValue build();
  }
}
