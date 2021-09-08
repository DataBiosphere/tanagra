package bio.terra.tanagra.service.databaseaccess.testing;

import bio.terra.tanagra.service.databaseaccess.CellValue;
import bio.terra.tanagra.service.search.DataType;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/** A {@link CellValue} for use in tests. */
@AutoValue
public abstract class TestCellValue implements CellValue {

  @Override
  public abstract DataType dataType();

  @Nullable
  abstract Long longVal();

  @Nullable
  abstract String stringVal();

  @Override
  public boolean isNull() {
    switch (dataType()) {
      case STRING:
        return stringVal() == null;
      case INT64:
        return longVal() == null;
      default:
        throw new UnsupportedOperationException(String.format("Unknown DataType: %s", dataType()));
    }
  }

  @Override
  public long getLong() {
    if (!dataType().equals(DataType.INT64)) {
      throw new ClassCastException("Not long DataType: " + dataType());
    }
    return longVal();
  }

  @Override
  public String getString() {
    if (!dataType().equals(DataType.STRING)) {
      throw new ClassCastException("Not string DataType: " + dataType());
    }
    if (stringVal() == null) {
      throw new NullPointerException();
    }
    return stringVal();
  }

  public static TestCellValue of(String value) {
    return builder().dataType(DataType.STRING).stringVal(value).build();
  }

  public static TestCellValue of(long value) {
    return builder().dataType(DataType.INT64).longVal(value).build();
  }

  public static TestCellValue ofNull(DataType type) {
    return builder().dataType(type).build();
  }

  public static Builder builder() {
    return new AutoValue_TestCellValue.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder dataType(DataType dataType);

    public abstract Builder longVal(Long longVal);

    public abstract Builder stringVal(String stringVal);

    public abstract TestCellValue build();
  }
}
