package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJsonOrProtoBytes;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.EntityGroupFilterUtils;
import bio.terra.tanagra.filterbuilder.impl.core.utils.GroupByCountSchemaUtils;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFTextSearch;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFUnhintedValue;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTTextSearch;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class TextSearchFilterBuilder extends FilterBuilder {
  public TextSearchFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    DTTextSearch.TextSearch textSearchSelectionData =
        deserializeData(selectionData.get(0).getPluginData());
    List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());
    if (textSearchSelectionData == null) {
      // Empty selection data = null filter for a cohort.
      return null;
    }

    // Pull the entity group, text search attribute from the config.
    CFTextSearch.TextSearch textSearchConfig = deserializeConfig();
    Pair<EntityGroup, Relationship> entityGroup =
        underlay.getRelationship(
            underlay.getEntity(textSearchConfig.getEntity()), underlay.getPrimaryEntity());
    CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup.getLeft();
    String textSearchAttrName =
        textSearchConfig.getSearchAttribute() == null
                || textSearchConfig.getSearchAttribute().isEmpty()
            ? null
            : textSearchConfig.getSearchAttribute();

    // Pull the criteria ids and text query from the selection data.
    List<Literal> criteriaIds =
        textSearchSelectionData.getCategoriesList().stream()
            .map(category -> Literal.forInt64(category.getValue().getInt64Value()))
            .collect(Collectors.toList());
    String textQuery = textSearchSelectionData.getQuery();

    // Build the sub-filter on the criteria entity.
    EntityFilter criteriaSubFilter =
        criteriaIds.isEmpty()
            ? null
            : EntityGroupFilterUtils.buildIdSubFilter(
                underlay, criteriaOccurrence.getCriteriaEntity(), criteriaIds);

    // Build the attribute modifier filters.
    Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity =
        EntityGroupFilterUtils.buildAttributeModifierFilters(
            underlay,
            criteriaSelector,
            modifiersSelectionData,
            criteriaOccurrence.getOccurrenceEntities());

    // Build the text search filter on each of the occurrence entities.
    if (textQuery != null && !textQuery.isEmpty() && !textQuery.isBlank()) {
      criteriaOccurrence
          .getOccurrenceEntities()
          .forEach(
              occurrenceEntity -> {
                List<EntityFilter> subFilters =
                    subFiltersPerOccurrenceEntity.containsKey(occurrenceEntity)
                        ? subFiltersPerOccurrenceEntity.get(occurrenceEntity)
                        : new ArrayList<>();
                Attribute textSearchAttr =
                    textSearchAttrName == null
                        ? null
                        : occurrenceEntity.getAttribute(textSearchAttrName);
                subFilters.add(
                    new TextSearchFilter(
                        underlay,
                        occurrenceEntity,
                        TextSearchFilter.TextSearchOperator.EXACT_MATCH,
                        textQuery,
                        textSearchAttr));
                subFiltersPerOccurrenceEntity.put(occurrenceEntity, subFilters);
              });
    }

    Optional<Pair<CFUnhintedValue.UnhintedValue, DTUnhintedValue.UnhintedValue>>
        groupByModifierConfigAndData =
            GroupByCountSchemaUtils.getModifier(criteriaSelector, modifiersSelectionData);
    if (groupByModifierConfigAndData.isEmpty()
        || groupByModifierConfigAndData.get().getRight() == null) {
      return new PrimaryWithCriteriaFilter(
          underlay,
          criteriaOccurrence,
          criteriaSubFilter,
          subFiltersPerOccurrenceEntity,
          null,
          null,
          null);
    }

    // Build the group by filter information.
    Map<Entity, List<Attribute>> groupByAttributesPerOccurrenceEntity =
        GroupByCountSchemaUtils.getGroupByAttributesPerOccurrenceEntity(
            underlay, groupByModifierConfigAndData, criteriaOccurrence.getOccurrenceEntities());
    DTUnhintedValue.UnhintedValue groupByModifierData =
        groupByModifierConfigAndData.get().getRight();
    return new PrimaryWithCriteriaFilter(
        underlay,
        criteriaOccurrence,
        criteriaSubFilter,
        subFiltersPerOccurrenceEntity,
        groupByAttributesPerOccurrenceEntity,
        GroupByCountSchemaUtils.toBinaryOperator(groupByModifierData.getOperator()),
        (int) groupByModifierData.getMin());
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    // Pull the entity group, text search attribute from the config.
    CFTextSearch.TextSearch textSearchConfig = deserializeConfig();
    Pair<EntityGroup, Relationship> entityGroup =
        underlay.getRelationship(
            underlay.getEntity(textSearchConfig.getEntity()), underlay.getPrimaryEntity());
    CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup.getLeft();
    String textSearchAttrName =
        textSearchConfig.getSearchAttribute() == null
                || textSearchConfig.getSearchAttribute().isEmpty()
            ? null
            : textSearchConfig.getSearchAttribute();

    // Pull text search filter from the plugin data.
    DTTextSearch.TextSearch textSearchSelectionData =
        selectionData.isEmpty() ? null : deserializeData(selectionData.get(0).getPluginData());
    List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());

    // Create an output for each of the occurrence entities.
    Map<Entity, List<EntityFilter>> filtersPerEntity;

    if (textSearchSelectionData == null) {
      // Empty selection data = occurrence entities with no filter.
      filtersPerEntity = new HashMap<>();
      criteriaOccurrence
          .getOccurrenceEntities()
          .forEach(occurrenceEntity -> filtersPerEntity.put(occurrenceEntity, new ArrayList<>()));
    } else {
      // Pull the criteria ids and text query from the selection data.
      List<Literal> criteriaIds =
          textSearchSelectionData.getCategoriesList().stream()
              .map(category -> Literal.forInt64(category.getValue().getInt64Value()))
              .collect(Collectors.toList());
      String textQuery = textSearchSelectionData.getQuery();

      // Build the criteria filters on each of the occurrence entities.
      filtersPerEntity =
          EntityGroupFilterUtils.addOccurrenceFiltersForDataFeature(
              underlay, criteriaOccurrence, criteriaIds);

      Map<Entity, List<EntityFilter>> attributeModifierFilters =
          EntityGroupFilterUtils.buildAttributeModifierFilters(
              underlay,
              criteriaSelector,
              modifiersSelectionData,
              criteriaOccurrence.getOccurrenceEntities());

      // Build the text search filter on each of the occurrence entities.
      if (textQuery != null && !textQuery.isEmpty() && !textQuery.isBlank()) {
        criteriaOccurrence
            .getOccurrenceEntities()
            .forEach(
                occurrenceEntity -> {
                  List<EntityFilter> subFilters =
                      filtersPerEntity.containsKey(occurrenceEntity)
                          ? filtersPerEntity.get(occurrenceEntity)
                          : new ArrayList<>();
                  Attribute textSearchAttr =
                      textSearchAttrName == null
                          ? null
                          : occurrenceEntity.getAttribute(textSearchAttrName);
                  subFilters.add(
                      new TextSearchFilter(
                          underlay,
                          occurrenceEntity,
                          TextSearchFilter.TextSearchOperator.EXACT_MATCH,
                          textQuery,
                          textSearchAttr));
                  if (attributeModifierFilters.containsKey(occurrenceEntity)) {
                    subFilters.addAll(attributeModifierFilters.get(occurrenceEntity));
                  }
                  filtersPerEntity.put(occurrenceEntity, subFilters);
                });
      }
    }

    // If there are multiple filters for a single entity, OR them together.
    return EntityGroupFilterUtils.mergeFiltersForDataFeature(
        filtersPerEntity, BooleanAndOrFilter.LogicalOperator.AND);
  }

  @Override
  public CFTextSearch.TextSearch deserializeConfig() {
    return deserializeFromJsonOrProtoBytes(
            criteriaSelector.getPluginConfig(), CFTextSearch.TextSearch.newBuilder())
        .build();
  }

  @Override
  public DTTextSearch.TextSearch deserializeData(String serialized) {
    return (serialized == null || serialized.isEmpty())
        ? null
        : deserializeFromJsonOrProtoBytes(serialized, DTTextSearch.TextSearch.newBuilder()).build();
  }
}
