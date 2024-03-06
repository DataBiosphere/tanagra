package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJson;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.EntityGroupFilterUtils;
import bio.terra.tanagra.filterbuilder.impl.core.utils.GroupByCountSchemaUtils;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "The config and data objects are deserialized by Jackson.")
public class EntityGroupFilterBuilder extends FilterBuilder {
  public EntityGroupFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    DTEntityGroup.EntityGroup entityGroupSelectionData =
        deserializeData(selectionData.get(0).getPluginData());
    List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());

    // We want to build one filter per entity group, not one filter per selected id.
    Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup =
        selectedIdsPerEntityGroup(underlay, entityGroupSelectionData);

    List<EntityFilter> entityFilters = new ArrayList<>();
    selectedIdsPerEntityGroup.entrySet().stream()
        .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
        .forEach(
            entry -> {
              EntityGroup entityGroup = entry.getKey();
              List<Literal> selectedIds = entry.getValue();
              switch (entityGroup.getType()) {
                case CRITERIA_OCCURRENCE:
                  entityFilters.add(
                      buildPrimaryWithCriteriaFilter(
                          underlay,
                          (CriteriaOccurrence) entityGroup,
                          selectedIds,
                          modifiersSelectionData));
                  break;
                case GROUP_ITEMS:
                  entityFilters.add(
                      buildGroupItemsFilter(
                          underlay, (GroupItems) entityGroup, selectedIds, modifiersSelectionData));
                  break;
                default:
                  throw new SystemException(
                      "Unsupported entity group type: " + entityGroup.getType());
              }
            });

    return entityFilters.size() == 1
        ? entityFilters.get(0)
        : new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, entityFilters);
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    if (selectionData.size() > 1) {
      throw new InvalidQueryException("Modifiers are not supported for data features");
    }
    DTEntityGroup.EntityGroup entityGroupSelectionData =
        deserializeData(selectionData.get(0).getPluginData());

    // We want to build filters per entity group, not per selected id.
    Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup =
        selectedIdsPerEntityGroup(underlay, entityGroupSelectionData);

    Map<Entity, List<EntityFilter>> filtersPerEntity = new HashMap<>();
    selectedIdsPerEntityGroup.entrySet().stream()
        .forEach(
            entry -> {
              EntityGroup entityGroup = entry.getKey();
              List<Literal> selectedIds = entry.getValue();
              switch (entityGroup.getType()) {
                case CRITERIA_OCCURRENCE:
                  EntityGroupFilterUtils.addOccurrenceFiltersForDataFeature(
                      underlay, (CriteriaOccurrence) entityGroup, selectedIds, filtersPerEntity);
                  break;
                case GROUP_ITEMS:
                  GroupItems groupItems = (GroupItems) entityGroup;
                  Entity notPrimaryEntity =
                      groupItems.getGroupEntity().isPrimary()
                          ? groupItems.getItemsEntity()
                          : groupItems.getGroupEntity();
                  List<EntityFilter> notPrimaryEntityFilters =
                      filtersPerEntity.containsKey(notPrimaryEntity)
                          ? filtersPerEntity.get(notPrimaryEntity)
                          : new ArrayList<>();
                  if (!selectedIds.isEmpty()) {
                    notPrimaryEntityFilters.add(
                        EntityGroupFilterUtils.buildIdSubFilter(
                            underlay, notPrimaryEntity, selectedIds));
                  }
                  filtersPerEntity.put(notPrimaryEntity, notPrimaryEntityFilters);
                  break;
                default:
                  throw new SystemException(
                      "Unsupported entity group type: " + entityGroup.getType());
              }
            });

    // If there are multiple filters for a single entity, OR them together.
    return EntityGroupFilterUtils.mergeFiltersForDataFeature(
        filtersPerEntity, BooleanAndOrFilter.LogicalOperator.OR);
  }

  private Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup(
      Underlay underlay, DTEntityGroup.EntityGroup entityGroupSelectionData) {
    Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup = new HashMap<>();
    for (DTEntityGroup.EntityGroup.Selection selectedId :
        entityGroupSelectionData.getSelectedList()) {
      EntityGroup entityGroup = underlay.getEntityGroup(selectedId.getEntityGroup());
      List<Literal> selectedIds =
          selectedIdsPerEntityGroup.containsKey(entityGroup)
              ? selectedIdsPerEntityGroup.get(entityGroup)
              : new ArrayList<>();
      if (selectedId.hasKey()) {
        selectedIds.add(Literal.forInt64(selectedId.getKey().getInt64Key()));
      }
      selectedIdsPerEntityGroup.put(entityGroup, selectedIds);
    }
    return selectedIdsPerEntityGroup;
  }

  private EntityFilter buildPrimaryWithCriteriaFilter(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      List<Literal> selectedIds,
      List<SelectionData> modifiersSelectionData) {
    // Build the criteria sub-filter.
    EntityFilter criteriaSubFilter =
        EntityGroupFilterUtils.buildIdSubFilter(
            underlay, criteriaOccurrence.getCriteriaEntity(), selectedIds);

    // Build the attribute modifier filters.
    Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity =
        EntityGroupFilterUtils.buildAttributeModifierFilters(
            underlay,
            criteriaSelector,
            modifiersSelectionData,
            criteriaOccurrence.getOccurrenceEntities());

    Optional<Pair<CFPlaceholder.Placeholder, DTUnhintedValue.UnhintedValue>>
        groupByModifierConfigAndData =
            GroupByCountSchemaUtils.getModifier(criteriaSelector, modifiersSelectionData);
    if (groupByModifierConfigAndData.isEmpty()) {
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
            underlay, groupByModifierConfigAndData);
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

  private EntityFilter buildGroupItemsFilter(
      Underlay underlay,
      GroupItems groupItems,
      List<Literal> selectedIds,
      List<SelectionData> modifiersSelectionData) {
    Entity notPrimaryEntity =
        groupItems.getGroupEntity().isPrimary()
            ? groupItems.getItemsEntity()
            : groupItems.getGroupEntity();

    // Build the sub-filters on the non-primary entity.
    List<EntityFilter> idFilterNonPrimaryEntity = new ArrayList<>();
    if (!selectedIds.isEmpty()) {
      idFilterNonPrimaryEntity.add(
          EntityGroupFilterUtils.buildIdSubFilter(underlay, notPrimaryEntity, selectedIds));
    }
    return EntityGroupFilterUtils.buildGroupItemsFilter(
        underlay, criteriaSelector, groupItems, idFilterNonPrimaryEntity, modifiersSelectionData);
  }

  @Override
  public CFPlaceholder.Placeholder deserializeConfig() {
    return deserializeFromJson(
            criteriaSelector.getPluginConfig(), CFPlaceholder.Placeholder.newBuilder())
        .build();
  }

  @Override
  public DTEntityGroup.EntityGroup deserializeData(String serialized) {
    return deserializeFromJson(serialized, DTEntityGroup.EntityGroup.newBuilder()).build();
  }
}
