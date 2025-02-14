package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.AttributeSchemaUtils;
import bio.terra.tanagra.filterbuilder.impl.core.utils.EntityGroupFilterUtils;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFMultiAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTMultiAttribute;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class MultiAttributeFilterBuilder
    extends FilterBuilder<CFMultiAttribute.MultiAttribute, DTMultiAttribute.MultiAttribute> {
  public MultiAttributeFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    DTMultiAttribute.MultiAttribute multiAttrSelectionData =
        deserializeData(selectionData.get(0).pluginData());
    List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());
    if (multiAttrSelectionData == null) {
      // Empty selection data = null filter for a cohort.
      return null;
    }

    // Pull the entity group from the config.
    CFMultiAttribute.MultiAttribute multiAttrConfig = deserializeConfig();
    Pair<EntityGroup, Relationship> entityGroup =
        underlay.getRelationship(
            underlay.getEntity(multiAttrConfig.getEntity()), underlay.getPrimaryEntity());
    GroupItems groupItems = (GroupItems) entityGroup.getLeft();

    Entity nonPrimaryEntity =
        groupItems.getGroupEntity().isPrimary()
            ? groupItems.getItemsEntity()
            : groupItems.getGroupEntity();

    // Build the attribute filters on the not-primary entity.
    List<EntityFilter> subFiltersNonPrimaryEntity =
        multiAttrSelectionData.getValueDataList().stream()
            .map(
                valueData ->
                    AttributeSchemaUtils.buildForEntity(
                        underlay,
                        nonPrimaryEntity,
                        nonPrimaryEntity.getAttribute(valueData.getAttribute()),
                        valueData))
            .toList();

    return EntityGroupFilterUtils.buildGroupItemsFilterFromSubFilters(
        underlay, criteriaSelector, groupItems, subFiltersNonPrimaryEntity, modifiersSelectionData);
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    // Pull the entity group from the config.
    CFMultiAttribute.MultiAttribute multiAttrConfig = deserializeConfig();
    Pair<EntityGroup, Relationship> entityGroup =
        underlay.getRelationship(
            underlay.getEntity(multiAttrConfig.getEntity()), underlay.getPrimaryEntity());
    GroupItems groupItems = (GroupItems) entityGroup.getLeft();
    Entity nonPrimaryEntity =
        groupItems.getGroupEntity().isPrimary()
            ? groupItems.getItemsEntity()
            : groupItems.getGroupEntity();

    // Empty selection data = not-primary entity with no filter.
    List<EntityFilter> subFiltersNonPrimaryEntity = new ArrayList<>();
    if (!selectionData.isEmpty()) {
      DTMultiAttribute.MultiAttribute multiAttrSelectionData =
          deserializeData(selectionData.get(0).pluginData());
      if (multiAttrSelectionData != null) {
        // Build the attribute filters on the not-primary entity.
        multiAttrSelectionData
            .getValueDataList()
            .forEach(
                valueData ->
                    subFiltersNonPrimaryEntity.add(
                        AttributeSchemaUtils.buildForEntity(
                            underlay,
                            nonPrimaryEntity,
                            nonPrimaryEntity.getAttribute(valueData.getAttribute()),
                            valueData)));
      }

      List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());
      Map<Entity, List<EntityFilter>> attributeModifierFilters =
          EntityGroupFilterUtils.buildAttributeModifierFilters(
              underlay, criteriaSelector, modifiersSelectionData, List.of(nonPrimaryEntity));
      if (attributeModifierFilters.containsKey(nonPrimaryEntity)) {
        subFiltersNonPrimaryEntity.addAll(attributeModifierFilters.get(nonPrimaryEntity));
      }
    }

    // Output the not primary entity.
    return EntityGroupFilterUtils.mergeFiltersForDataFeature(
        Map.of(nonPrimaryEntity, subFiltersNonPrimaryEntity),
        BooleanAndOrFilter.LogicalOperator.AND);
  }

  @Override
  public CFMultiAttribute.MultiAttribute deserializeConfig() {
    return deserializeFromJsonOrProtoBytes(
            criteriaSelector.getPluginConfig(), CFMultiAttribute.MultiAttribute.newBuilder())
        .build();
  }

  @Override
  public DTMultiAttribute.MultiAttribute deserializeData(String serialized) {
    return StringUtils.isEmpty(serialized)
        ? null
        : deserializeFromJsonOrProtoBytes(serialized, DTMultiAttribute.MultiAttribute.newBuilder())
            .build();
  }
}
