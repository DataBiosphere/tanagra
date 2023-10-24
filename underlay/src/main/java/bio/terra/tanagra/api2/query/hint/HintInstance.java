package bio.terra.tanagra.api2.query.hint;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.underlay2.Attribute;
import bio.terra.tanagra.underlay2.Entity;
import bio.terra.tanagra.underlay2.indexschema.EntityLevelDisplayHints;
import bio.terra.tanagra.underlay2.indexschema.InstanceLevelDisplayHints;
import com.google.common.collect.ImmutableMap;
import java.util.*;
import javax.annotation.Nullable;

public final class HintInstance {
  private final Attribute attribute;
  private final @Nullable Double min;
  private final @Nullable Double max;
  private final Map<ValueDisplay, Long> enumValueCounts;

  private HintInstance(
      Attribute attribute,
      @Nullable Double min,
      @Nullable Double max,
      @Nullable Map<ValueDisplay, Long> enumValueCounts) {
    this.attribute = attribute;
    this.min = min;
    this.max = max;
    this.enumValueCounts =
        enumValueCounts == null ? new HashMap<>() : new HashMap<>(enumValueCounts);
  }

  public static void fromRowResult(
      RowResult rowResult, List<HintInstance> hintInstances, Entity entity, boolean isEntityLevel) {
    String attributeName =
        rowResult
            .get(getAttributeColumnName(isEntityLevel))
            .getLiteral()
            .orElseThrow()
            .getStringVal();
    Attribute attribute = entity.getAttribute(attributeName);

    OptionalDouble min = rowResult.get(getMinColumnName(isEntityLevel)).getDouble();
    OptionalDouble max = rowResult.get(getMaxColumnName(isEntityLevel)).getDouble();
    if (min.isPresent() && max.isPresent()) {
      // This is a numeric range hint, which is contained in a single row.
      hintInstances.add(new HintInstance(attribute, min.getAsDouble(), max.getAsDouble(), null));
    } else {
      // This is part of an enum values hint, which is spread across multiple rows, one per value.
      Literal enumVal =
          rowResult
              .get(getEnumValueColumnName(isEntityLevel))
              .getLiteral()
              .orElse(new Literal(null));
      String enumDisplay =
          rowResult.get(getEnumDisplayColumnName(isEntityLevel)).getString().orElse(null);
      Long enumCount = rowResult.get(getEnumCountColumnName(isEntityLevel)).getLong().getAsLong();
      ValueDisplay enumValDisplay = new ValueDisplay(enumVal, enumDisplay);

      Optional<HintInstance> existingHintInstance =
          hintInstances.stream().filter(hi -> hi.getAttribute().equals(attribute)).findFirst();
      if (existingHintInstance.isEmpty()) {
        // Add a new hint instance.
        hintInstances.add(
            new HintInstance(attribute, null, null, Map.of(enumValDisplay, enumCount)));
      } else {
        // Update an existing hint instance.
        existingHintInstance.get().addEnumValueCount(enumValDisplay, enumCount);
      }
    }
  }

  private static String getAttributeColumnName(boolean isEntityLevel) {
    return isEntityLevel
        ? EntityLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName()
        : InstanceLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName();
  }

  private static String getMinColumnName(boolean isEntityLevel) {
    return isEntityLevel
        ? EntityLevelDisplayHints.Column.MIN.getSchema().getColumnName()
        : InstanceLevelDisplayHints.Column.MIN.getSchema().getColumnName();
  }

  private static String getMaxColumnName(boolean isEntityLevel) {
    return isEntityLevel
        ? EntityLevelDisplayHints.Column.MAX.getSchema().getColumnName()
        : InstanceLevelDisplayHints.Column.MAX.getSchema().getColumnName();
  }

  private static String getEnumValueColumnName(boolean isEntityLevel) {
    return isEntityLevel
        ? EntityLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName()
        : InstanceLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName();
  }

  private static String getEnumDisplayColumnName(boolean isEntityLevel) {
    return isEntityLevel
        ? EntityLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName()
        : InstanceLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName();
  }

  private static String getEnumCountColumnName(boolean isEntityLevel) {
    return isEntityLevel
        ? EntityLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName()
        : InstanceLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName();
  }

  private void addEnumValueCount(ValueDisplay enumValue, Long count) {
    enumValueCounts.put(enumValue, count);
  }

  public Attribute getAttribute() {
    return attribute;
  }

  public Double getMin() {
    return min;
  }

  public Double getMax() {
    return max;
  }

  public ImmutableMap<ValueDisplay, Long> getEnumValueCounts() {
    return ImmutableMap.copyOf(enumValueCounts);
  }
}
