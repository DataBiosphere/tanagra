package bio.terra.tanagra.filterbuilder.impl.utils;

import static bio.terra.tanagra.filterbuilder.SchemaUtils.toLiteral;
import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJson;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.proto.criteriaselector.DataRangeOuterClass.DataRange;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class AttributeSchemaUtils {
  private AttributeSchemaUtils() {}

  public static EntityFilter buildForEntity(
      Underlay underlay,
      Entity entity,
      CFPlaceholder.Placeholder config,
      DTAttribute.Attribute data) {
    Attribute attribute = entity.getAttribute(config.getAttribute());
    if (!data.getSelectedList().isEmpty()) {
      // Enum value filter.
      return data.getSelectedCount() == 1
          ? new AttributeFilter(
              underlay,
              entity,
              attribute,
              BinaryOperator.EQUALS,
              toLiteral(data.getSelected(0).getValue()))
          : new AttributeFilter(
              underlay,
              entity,
              attribute,
              NaryOperator.IN,
              data.getSelectedList().stream()
                  .map(selected -> toLiteral(selected.getValue()))
                  .collect(Collectors.toList()));
    } else {
      // Numeric range filter.
      List<EntityFilter> rangeFilters = new ArrayList<>();
      for (DataRange range : data.getDataRangesList()) {
        rangeFilters.add(
            new AttributeFilter(
                underlay,
                entity,
                attribute,
                NaryOperator.BETWEEN,
                List.of(Literal.forDouble(range.getMin()), Literal.forDouble(range.getMax()))));
      }
      return rangeFilters.size() == 1
          ? rangeFilters.get(0)
          : new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, rangeFilters);
    }
  }

  public static CFPlaceholder.Placeholder deserializeConfig(String serialized) {
    return deserializeFromJson(serialized, CFPlaceholder.Placeholder.newBuilder()).build();
  }

  public static DTAttribute.Attribute deserializeData(String serialized) {
    return deserializeFromJson(serialized, DTAttribute.Attribute.newBuilder()).build();
  }
}
