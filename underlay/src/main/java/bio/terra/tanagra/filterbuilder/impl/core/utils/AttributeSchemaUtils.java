package bio.terra.tanagra.filterbuilder.impl.core.utils;

import static bio.terra.tanagra.filterbuilder.SchemaUtils.toLiteral;
import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.proto.criteriaselector.DataRangeOuterClass.DataRange;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public final class AttributeSchemaUtils {
  private AttributeSchemaUtils() {}

  public static EntityFilter buildForEntity(
      Underlay underlay,
      Entity entity,
      Attribute attribute,
      ValueDataOuterClass.ValueData valueData) {
    return buildForEntity(underlay, entity, attribute, convertToAttrDataSchema(valueData));
  }

  public static EntityFilter buildForEntity(
      Underlay underlay, Entity entity, Attribute attribute, DTAttribute.Attribute data) {
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

  public static List<Pair<CFAttribute.Attribute, DTAttribute.Attribute>> getModifiers(
      CriteriaSelector criteriaSelector, List<SelectionData> selectionData) {
    return selectionData.stream()
        .filter(
            modifierSelectionData ->
                SZCorePlugin.ATTRIBUTE
                    .getIdInConfig()
                    .equals(
                        criteriaSelector
                            .getModifier(modifierSelectionData.getModifierName())
                            .getPlugin()))
        .map(
            modifierSelectionData -> {
              CriteriaSelector.Modifier modifierDefn =
                  criteriaSelector.getModifier(modifierSelectionData.getModifierName());
              CFAttribute.Attribute modifierConfig =
                  deserializeConfig(modifierDefn.getPluginConfig());
              DTAttribute.Attribute modifierData =
                  deserializeData(modifierSelectionData.getPluginData());
              return Pair.of(modifierConfig, modifierData);
            })
        .collect(Collectors.toList());
  }

  public static CFAttribute.Attribute deserializeConfig(String serialized) {
    return deserializeFromJsonOrProtoBytes(serialized, CFAttribute.Attribute.newBuilder()).build();
  }

  public static DTAttribute.Attribute deserializeData(String serialized) {
    return deserializeFromJsonOrProtoBytes(serialized, DTAttribute.Attribute.newBuilder()).build();
  }

  private static DTAttribute.Attribute convertToAttrDataSchema(
      ValueDataOuterClass.ValueData valueData) {
    // Convert the value_data schema into the attribute plugin data schema, so we can share
    // processing code.
    DTAttribute.Attribute.Builder attrData = DTAttribute.Attribute.newBuilder();
    valueData.getSelectedList().stream()
        .forEach(
            valueDataSelection -> {
              DTAttribute.Attribute.Selection attrSelection =
                  DTAttribute.Attribute.Selection.newBuilder()
                      .setValue(valueDataSelection.getValue())
                      .setName(valueDataSelection.getName())
                      .build();
              attrData.addSelected(attrSelection);
            });
    attrData.addDataRanges(valueData.getRange());
    return attrData.build();
  }
}
