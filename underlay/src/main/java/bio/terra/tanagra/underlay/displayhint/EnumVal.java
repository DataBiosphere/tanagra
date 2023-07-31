package bio.terra.tanagra.underlay.displayhint;

import bio.terra.tanagra.serialization.displayhint.UFEnumVal;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.Objects;

public class EnumVal {
  private final ValueDisplay valueDisplay;
  private final long count;

  public EnumVal(ValueDisplay valueDisplay, long count) {
    this.valueDisplay = valueDisplay;
    this.count = count;
  }

  public static EnumVal fromSerialized(UFEnumVal serialized) {
    return new EnumVal(ValueDisplay.fromSerialized(serialized.getEnumVal()), serialized.getCount());
  }

  public ValueDisplay getValueDisplay() {
    return valueDisplay;
  }

  public long getCount() {
    return count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnumVal enumVal = (EnumVal) o;
    return count == enumVal.count && valueDisplay.equals(enumVal.valueDisplay);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valueDisplay, count);
  }
}
