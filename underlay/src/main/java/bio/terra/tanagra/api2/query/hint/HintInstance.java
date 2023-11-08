package bio.terra.tanagra.api2.query.hint;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.underlay2.entitymodel.Attribute;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

public final class HintInstance {
  private final Attribute attribute;
  private final boolean isRangeHint;
  private final double min;
  private final double max;
  private final Map<ValueDisplay, Long> enumValueCounts;

  public HintInstance(Attribute attribute, double min, double max) {
    this.attribute = attribute;
    this.isRangeHint = true;
    this.min = min;
    this.max = max;
    this.enumValueCounts = Map.of();
  }

  public HintInstance(Attribute attribute, Map<ValueDisplay, Long> enumValueCounts) {
    this.attribute = attribute;
    this.isRangeHint = false;
    this.min = -1;
    this.max = -1;
    this.enumValueCounts = Map.copyOf(enumValueCounts);
  }

  public void addEnumValueCount(ValueDisplay enumValue, Long count) {
    enumValueCounts.put(enumValue, count);
  }

  public Attribute getAttribute() {
    return attribute;
  }

  public boolean isRangeHint() {
    return isRangeHint;
  }

  public boolean isEnumHint() {
    return !isRangeHint;
  }

  public double getMin() {
    return min;
  }

  public double getMax() {
    return max;
  }

  public ImmutableMap<ValueDisplay, Long> getEnumValueCounts() {
    return ImmutableMap.copyOf(enumValueCounts);
  }
}
