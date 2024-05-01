package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.exception.InvalidQueryException;
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
import org.apache.commons.lang3.tuple.Pair;

public class MultiAttributeFilterBuilder extends FilterBuilder {
  public MultiAttributeFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    DTMultiAttribute.MultiAttribute multiAttrSelectionData =
        deserializeData(selectionData.get(0).getPluginData());
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
    Entity notPrimaryEntity =
        groupItems.getGroupEntity().isPrimary()
            ? groupItems.getItemsEntity()
            : groupItems.getGroupEntity();

    // Build the attribute filters on the not-primary entity.
    List<EntityFilter> subFiltersNotPrimaryEntity = new ArrayList<>();
    multiAttrSelectionData
        .getValueDataList()
        .forEach(
            valueData ->
                subFiltersNotPrimaryEntity.add(
                    AttributeSchemaUtils.buildForEntity(
                        underlay,
                        notPrimaryEntity,
                        notPrimaryEntity.getAttribute(valueData.getAttribute()),
                        valueData)));
    return EntityGroupFilterUtils.buildGroupItemsFilter(
        underlay, criteriaSelector, groupItems, subFiltersNotPrimaryEntity, modifiersSelectionData);
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    if (selectionData.size() > 1) {
      throw new InvalidQueryException("Modifiers are not supported for data features");
    }

    // Pull the entity group from the config.
    CFMultiAttribute.MultiAttribute multiAttrConfig = deserializeConfig();
    Pair<EntityGroup, Relationship> entityGroup =
        underlay.getRelationship(
            underlay.getEntity(multiAttrConfig.getEntity()), underlay.getPrimaryEntity());
    GroupItems groupItems = (GroupItems) entityGroup.getLeft();
    Entity notPrimaryEntity =
        groupItems.getGroupEntity().isPrimary()
            ? groupItems.getItemsEntity()
            : groupItems.getGroupEntity();

    // Empty selection data = not-primary entity with no filter.
    List<EntityFilter> subFiltersNotPrimaryEntity = new ArrayList<>();
    if (!selectionData.isEmpty()) {
      DTMultiAttribute.MultiAttribute multiAttrSelectionData =
          deserializeData(selectionData.get(0).getPluginData());
      if (multiAttrSelectionData != null) {
        // Build the attribute filters on the not-primary entity.
        multiAttrSelectionData
            .getValueDataList()
            .forEach(
                valueData ->
                    subFiltersNotPrimaryEntity.add(
                        AttributeSchemaUtils.buildForEntity(
                            underlay,
                            notPrimaryEntity,
                            notPrimaryEntity.getAttribute(valueData.getAttribute()),
                            valueData)));
      }
    }

    // Output the not primary entity.
    return EntityGroupFilterUtils.mergeFiltersForDataFeature(
        Map.of(notPrimaryEntity, subFiltersNotPrimaryEntity),
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
    return (serialized == null || serialized.isEmpty())
        ? null
        : deserializeFromJsonOrProtoBytes(serialized, DTMultiAttribute.MultiAttribute.newBuilder())
            .build();
  }
}
