package bio.terra.tanagra.api2.query.hint;

import bio.terra.tanagra.api2.query.ValueDisplay;
import javax.annotation.Nullable;

public class Hint {
  private final String attribute;
  private final double min;
  private final double max;
  private @Nullable final ValueDisplay enumVal;
  private final long enumCount;

  public Hint(String attribute, double min, double max) {
    this.attribute = attribute;
    this.min = min;
    this.max = max;
    this.enumVal = null;
    this.enumCount = -1;
  }

  public Hint(String attribute, ValueDisplay enumVal, long enumCount) {
    this.attribute = attribute;
    this.min = -1;
    this.max = -1;
    this.enumVal = enumVal;
    this.enumCount = enumCount;
  }

  public boolean isRangeHint() {
    return enumVal == null;
  }

  public String getAttribute() {
    return attribute;
  }

  public double getMin() {
    return min;
  }

  public double getMax() {
    return max;
  }

  public ValueDisplay getEnumVal() {
    return enumVal;
  }

  public long getEnumCount() {
    return enumCount;
  }
}
