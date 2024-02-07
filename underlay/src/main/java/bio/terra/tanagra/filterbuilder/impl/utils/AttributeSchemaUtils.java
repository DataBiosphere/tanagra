package bio.terra.tanagra.filterbuilder.impl.utils;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.filterbuilder.schema.attribute.PSAttributeConfig;
import bio.terra.tanagra.filterbuilder.schema.attribute.PSAttributeData;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.utils.JacksonMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class AttributeSchemaUtils {
  private AttributeSchemaUtils() {}

  public static EntityFilter buildForEntity(
      Underlay underlay, Entity entity, PSAttributeConfig config, PSAttributeData data) {
    Attribute attribute = entity.getAttribute(config.attribute);
    if (!data.enumVals.isEmpty()) {
      // Enum value filter.
      return data.enumVals.size() == 1
          ? new AttributeFilter(
              underlay, entity, attribute, BinaryOperator.EQUALS, data.enumVals.get(0).getValue())
          : new AttributeFilter(
              underlay,
              entity,
              attribute,
              NaryOperator.IN,
              data.enumVals.stream().map(ValueDisplay::getValue).collect(Collectors.toList()));
    } else {
      // Numeric range filter.
      List<AttributeFilter> rangeFilters = new ArrayList<>();
      for (PSAttributeData.Range range : data.ranges) {
        rangeFilters.add(
            new AttributeFilter(
                underlay, entity, attribute, NaryOperator.BETWEEN, List.of(range.min, range.max)));
      }
      return rangeFilters.size() == 1
          ? rangeFilters.get(0)
          : new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, rangeFilters);
    }
  }

  public static PSAttributeData deserializeData(String serialized) {
    return JacksonMapper.deserializeJavaObject(
        serialized,
        PSAttributeData.class,
        (jpEx) -> new InvalidConfigException("Error deserializing criteria selector data", jpEx));
  }
}
