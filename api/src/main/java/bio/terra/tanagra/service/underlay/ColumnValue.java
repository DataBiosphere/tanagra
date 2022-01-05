package bio.terra.tanagra.service.underlay;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** A value to filter a SQL column within a table in an underlay. */
@AutoValue
public abstract class ColumnValue {
  @Nullable
  public abstract Long int64Val();

  @Nullable
  public abstract String stringVal();

  public static ColumnValue.Builder builder() {
    return new AutoValue_ColumnValue.Builder();
  }

  /** A builder for {@link ColumnValue}. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder int64Val(Long int64Val);

    public abstract Builder stringVal(String stringVal);

    public abstract ColumnValue build();
  }
}
