package bio.terra.tanagra.filterbuilder.impl.core.utils;

import static bio.terra.tanagra.api.filter.BooleanAndOrFilter.newBooleanAndOrFilter;
import static bio.terra.tanagra.filterbuilder.SchemaUtils.toLiteral;
import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter.LogicalOperator;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.proto.criteriaselector.DataRangeOuterClass.DataRange;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass;
import bio.terra.tanagra.proto.criteriaselector.ValueOuterClass.Value;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public final class AttributeSchemaUtils {
  public static final String IGNORED_ATTRIBUTE_NAME_UI_USE_ONLY = "t_any";

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
    if (data == null || (data.getSelectedList().isEmpty() && data.getDataRangesList().isEmpty())) {
      return null;
    }
    if (!data.getSelectedList().isEmpty()) {
      // Enum value filter.
      List<EntityFilter> filtersToOr = new ArrayList<>();
      List<Literal> enumLiterals = new ArrayList<>();
      data.getSelectedList()
          .forEach(
              selected -> {
                Value selectedValue = selected.getValue();
                if (attribute.getEmptyValueDisplay().equals(selectedValue.getStringValue())) {
                  // empty value is selected: entries with no value for this attribute
                  filtersToOr.add(
                      new AttributeFilter(underlay, entity, attribute, UnaryOperator.IS_NULL));
                } else {
                  enumLiterals.add(toLiteral(selectedValue, attribute.getDataType()));
                }
              });

      if (!enumLiterals.isEmpty()) {
        filtersToOr.add(
            enumLiterals.size() == 1
                ? new AttributeFilter(
                    underlay, entity, attribute, BinaryOperator.EQUALS, enumLiterals.get(0))
                : new AttributeFilter(underlay, entity, attribute, NaryOperator.IN, enumLiterals));
      }
      return newBooleanAndOrFilter(LogicalOperator.OR, filtersToOr);

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
      return newBooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, rangeFilters);
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
                            .getModifier(modifierSelectionData.modifierName())
                            .plugin()))
        .map(
            modifierSelectionData -> {
              CriteriaSelector.Modifier modifierDefn =
                  criteriaSelector.getModifier(modifierSelectionData.modifierName());
              CFAttribute.Attribute modifierConfig = deserializeConfig(modifierDefn.pluginConfig());
              DTAttribute.Attribute modifierData =
                  deserializeData(modifierSelectionData.pluginData());
              return Pair.of(modifierConfig, modifierData);
            })
        .collect(Collectors.toList());
  }

  public static CFAttribute.Attribute deserializeConfig(String serialized) {
    return deserializeFromJsonOrProtoBytes(serialized, CFAttribute.Attribute.newBuilder()).build();
  }

  public static DTAttribute.Attribute deserializeData(String serialized) {
    return StringUtils.isEmpty(serialized)
        ? null
        : deserializeFromJsonOrProtoBytes(serialized, DTAttribute.Attribute.newBuilder()).build();
  }

  private static DTAttribute.Attribute convertToAttrDataSchema(
      ValueDataOuterClass.ValueData valueData) {
    // Convert the value_data schema into the attribute plugin data schema, so we can share
    // processing code.
    DTAttribute.Attribute.Builder attrData = DTAttribute.Attribute.newBuilder();
    valueData
        .getSelectedList()
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
