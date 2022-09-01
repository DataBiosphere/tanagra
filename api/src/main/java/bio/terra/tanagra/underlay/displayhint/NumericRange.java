package bio.terra.tanagra.underlay.displayhint;

import bio.terra.tanagra.serialization.displayhint.UFNumericRange;
import bio.terra.tanagra.underlay.DisplayHint;

public final class NumericRange extends DisplayHint {
  private final Long minVal;
  private final Long maxVal;

  public NumericRange(Long minVal, Long maxVal) {
    this.minVal = minVal;
    this.maxVal = maxVal;
  }

  public static NumericRange fromSerialized(UFNumericRange serialized) {
    if (serialized.getMinVal() == null) {
      throw new IllegalArgumentException("Numeric range minimum value is undefined");
    }
    if (serialized.getMaxVal() == null) {
      throw new IllegalArgumentException("Numeric range maximum value is undefined");
    }
    return new NumericRange(serialized.getMinVal(), serialized.getMaxVal());
  }

  @Override
  public Type getType() {
    return Type.RANGE;
  }

  public Long getMinVal() {
    return minVal;
  }

  public Long getMaxVal() {
    return maxVal;
  }
}
