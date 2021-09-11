package bio.terra.tanagra.service.databaseaccess.testing;

import bio.terra.tanagra.model.DataType;
import bio.terra.tanagra.service.databaseaccess.CellValue;
import com.google.auto.value.AutoValue;
import java.util.Optional;
import java.util.OptionalLong;

/** A {@link CellValue} for use in tests. */
@AutoValue
public abstract class TestCellValue implements CellValue {

  @Override
  public abstract DataType dataType();

  abstract OptionalLong longVal();

  abstract Optional<String> stringVal();

  @Override
  public OptionalLong getLong() {
    assertDataTypeIs(DataType.INT64);
    return longVal();
  }

  @Override
  public Optional<String> getString() {
    assertDataTypeIs(DataType.STRING);
    return stringVal();
  }

  /**
   * Checks that the {@link #dataType()} is what's expected, or else throws a {@link
   * ClassCastException}.
   */
  private void assertDataTypeIs(DataType expected) {
    if (!dataType().equals(expected)) {
      throw new ClassCastException(
          String.format("DataType is %s, not the expected %s", dataType(), expected));
    }
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
