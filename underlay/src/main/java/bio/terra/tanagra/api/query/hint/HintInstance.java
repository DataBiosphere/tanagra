package bio.terra.tanagra.api.query.hint;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.query.sql.SqlQueryResult;
import bio.terra.tanagra.query.sql.SqlRowResult;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

public final class HintInstance {
  private final Attribute attribute;
  private final boolean isRangeHint;
  private final double min;
  private final double max;
  private final Map<ValueDisplay, Long> enumValueCounts;
  // TODO(dexamundsen): BENCH-5178: use attribute.getEmptyValueDisplay()
  private static final String ATTR_EMPTY_VALUE_DISPLAY = "n/a";

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

  public void addEnumValueCount(ValueDisplay valueDisplay, Long count) {
    this.enumValueCounts.put(valueDisplay, count);
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

  public Optional<String> getEnumDisplay(Literal enumValue) {
    return enumValueCounts.keySet().stream()
        .filter(valueDisplay -> valueDisplay.getValue().equals(enumValue))
        .map(ValueDisplay::getDisplay)
        .findAny();
  }

  public static List<HintInstance> rangeInstances(
      SqlQueryResult sqlQueryResult, Attribute attribute, String minAlias, String maxAlias) {
    List<HintInstance> hintInstances = new ArrayList<>();
    sqlQueryResult
        .rowResults()
        .iterator()
        .forEachRemaining(
            sqlRowResult ->
                newRangeInstance(sqlRowResult, attribute, minAlias, maxAlias)
                    .ifPresent(hintInstances::add));
    return hintInstances;
  }

  public static Optional<HintInstance> newRangeInstance(
      SqlRowResult sqlRowResult, Attribute attribute, String minAlias, String maxAlias) {
    Double min = sqlRowResult.get(minAlias, DataType.DOUBLE).getDoubleVal();
    Double max = sqlRowResult.get(maxAlias, DataType.DOUBLE).getDoubleVal();
    return (min != null && max != null)
        ? Optional.of(new HintInstance(attribute, min, max))
        : Optional.empty();
  }

  public static List<HintInstance> valueDisplayInstance(
      SqlQueryResult sqlQueryResult,
      Attribute attribute,
      String valueAlias,
      String displayAlias,
      String countAlias) {
    Map<ValueDisplay, Long> attrEnumValues = new HashMap<>();
    sqlQueryResult
        .rowResults()
        .iterator()
        .forEachRemaining(
            sqlRowResult -> {
              Entry<ValueDisplay, Long> entry =
                  newValueDisplayInstance(
                      sqlRowResult, attribute, valueAlias, displayAlias, countAlias);
              attrEnumValues.put(entry.getKey(), entry.getValue());
            });
    return List.of(new HintInstance(attribute, attrEnumValues));
  }

  public static Entry<ValueDisplay, Long> newValueDisplayInstance(
      SqlRowResult sqlRowResult,
      Attribute attribute,
      String valueAlias,
      String displayAlias,
      String countAlias) {
    Literal enumVal = sqlRowResult.get(valueAlias, DataType.INT64);
    String enumDisplay =
        Optional.ofNullable(sqlRowResult.get(displayAlias, DataType.STRING).getStringVal())
            .orElse(ATTR_EMPTY_VALUE_DISPLAY);
    Long enumCount = sqlRowResult.get(countAlias, DataType.INT64).getInt64Val();
    return new AbstractMap.SimpleEntry<>(new ValueDisplay(enumVal, enumDisplay), enumCount);
  }

  public static List<HintInstance> repeatedStringInstance(
      SqlQueryResult sqlQueryResult, Attribute attribute, String valAlias, String countAlias) {
    Map<ValueDisplay, Long> attrEnumValues = new HashMap<>();
    sqlQueryResult
        .rowResults()
        .iterator()
        .forEachRemaining(
            sqlRowResult -> {
              Entry<ValueDisplay, Long> entry =
                  newRepeatedStringEntry(sqlRowResult, attribute, valAlias, countAlias);
              attrEnumValues.put(entry.getKey(), entry.getValue());
            });
    return List.of(new HintInstance(attribute, attrEnumValues));
  }

  public static Entry<ValueDisplay, Long> newRepeatedStringEntry(
      SqlRowResult sqlRowResult, Attribute attribute, String valAlias, String countAlias) {
    Literal enumVal = sqlRowResult.get(valAlias, DataType.STRING);
    String enumDisplay =
        Optional.ofNullable(enumVal.getStringVal()).orElse(ATTR_EMPTY_VALUE_DISPLAY);
    Long enumCount = sqlRowResult.get(countAlias, DataType.INT64).getInt64Val();
    return new AbstractMap.SimpleEntry<>(new ValueDisplay(enumVal, enumDisplay), enumCount);
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
