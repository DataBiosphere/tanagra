package bio.terra.tanagra.api.query.hint;

import bio.terra.tanagra.api.query.ValueDisplay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    this.enumValueCounts = new HashMap<>(enumValueCounts);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HintInstance that = (HintInstance) o;
    return isRangeHint == that.isRangeHint
        && Double.compare(that.min, min) == 0
        && Double.compare(that.max, max) == 0
        && attribute.equals(that.attribute)
        && enumValueCounts.equals(that.enumValueCounts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attribute, isRangeHint, min, max, enumValueCounts);
  }
}
