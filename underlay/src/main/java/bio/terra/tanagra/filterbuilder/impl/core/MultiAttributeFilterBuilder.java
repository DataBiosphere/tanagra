package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJson;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.AttributeSchemaUtils;
import bio.terra.tanagra.filterbuilder.impl.core.utils.EntityGroupFilterUtils;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTMultiAttribute;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MultiAttributeFilterBuilder extends FilterBuilder {
  public MultiAttributeFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    DTMultiAttribute.MultiAttribute multiAttrSelectionData =
        deserializeData(selectionData.get(0).getPluginData());
    List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());

    // Pull the entity group from the config.
    CFPlaceholder.Placeholder multiAttrConfig = deserializeConfig();
    GroupItems groupItems =
        (GroupItems) underlay.getEntityGroup(multiAttrConfig.getEntityGroupMultiAttr());
    Entity notPrimaryEntity =
        groupItems.getGroupEntity().isPrimary()
            ? groupItems.getItemsEntity()
            : groupItems.getGroupEntity();

    // Build the attribute filters on the not-primary entity.
    List<EntityFilter> subFiltersNotPrimaryEntity = new ArrayList<>();
    multiAttrSelectionData.getValueDataList().stream()
        .forEach(
            valueData -> {
              // Convert the multi-attribute value_data into the attribute plugin data schema, so we
              // can share processing code.
              DTAttribute.Attribute attrSchema = convertToAttrDataSchema(valueData);
              EntityFilter attrFilter =
                  AttributeSchemaUtils.buildForEntity(
                      underlay,
                      notPrimaryEntity,
                      notPrimaryEntity.getAttribute(valueData.getAttribute()),
                      attrSchema);
              subFiltersNotPrimaryEntity.add(attrFilter);
            });
    return EntityGroupFilterUtils.buildGroupItemsFilter(
        underlay, criteriaSelector, groupItems, subFiltersNotPrimaryEntity, modifiersSelectionData);
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    if (selectionData.size() > 1) {
      throw new InvalidQueryException("Modifiers are not supported for data features");
    }
    DTMultiAttribute.MultiAttribute multiAttrSelectionData =
        deserializeData(selectionData.get(0).getPluginData());

    // Pull the entity group from the config.
    CFPlaceholder.Placeholder multiAttrConfig = deserializeConfig();
    GroupItems groupItems =
        (GroupItems) underlay.getEntityGroup(multiAttrConfig.getEntityGroupMultiAttr());
    Entity notPrimaryEntity =
        groupItems.getGroupEntity().isPrimary()
            ? groupItems.getItemsEntity()
            : groupItems.getGroupEntity();

    // Build the attribute filters on the not-primary entity.
    List<EntityFilter> subFiltersNotPrimaryEntity = new ArrayList<>();
    multiAttrSelectionData.getValueDataList().stream()
        .forEach(
            valueData -> {
              // Convert the multi-attribute value_data into the attribute plugin data schema, so we
              // can share processing code.
              DTAttribute.Attribute attrSchema = convertToAttrDataSchema(valueData);
              EntityFilter attrFilter =
                  AttributeSchemaUtils.buildForEntity(
                      underlay,
                      notPrimaryEntity,
                      notPrimaryEntity.getAttribute(valueData.getAttribute()),
                      attrSchema);
              subFiltersNotPrimaryEntity.add(attrFilter);
            });

    // Output the not primary entity.
    return EntityGroupFilterUtils.mergeFiltersForDataFeature(
        Map.of(notPrimaryEntity, subFiltersNotPrimaryEntity),
        BooleanAndOrFilter.LogicalOperator.AND);
  }

  @Override
  public CFPlaceholder.Placeholder deserializeConfig() {
    return deserializeFromJson(
            criteriaSelector.getPluginConfig(), CFPlaceholder.Placeholder.newBuilder())
        .build();
  }

  @Override
  public DTMultiAttribute.MultiAttribute deserializeData(String serialized) {
    return deserializeFromJson(serialized, DTMultiAttribute.MultiAttribute.newBuilder()).build();
  }

  private static DTAttribute.Attribute convertToAttrDataSchema(
      ValueDataOuterClass.ValueData valueData) {
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
