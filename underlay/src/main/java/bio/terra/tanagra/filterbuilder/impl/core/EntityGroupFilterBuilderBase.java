package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.filterbuilder.impl.core.utils.AttributeSchemaUtils.IGNORED_ATTRIBUTE_NAME_UI_USE_ONLY;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.AttributeSchemaUtils;
import bio.terra.tanagra.filterbuilder.impl.core.utils.EntityGroupFilterUtils;
import bio.terra.tanagra.filterbuilder.impl.core.utils.GroupByCountSchemaUtils;
import bio.terra.tanagra.proto.criteriaselector.ValueDataOuterClass;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFUnhintedValue;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "The config and data objects are deserialized by Jackson.")
public abstract class EntityGroupFilterBuilderBase extends FilterBuilder {
  public EntityGroupFilterBuilderBase(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    String criteriaSelectionData = selectionData.get(0).getPluginData();
    List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());

    // We want to build one filter per entity group, not one filter per selected id.
    Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup =
        selectedIdsPerEntityGroup(underlay, criteriaSelectionData);
    if (selectedIdsPerEntityGroup.isEmpty() && modifiersSelectionData.isEmpty()) {
      // Empty selection data = null filter for a cohort.
      return null;
    }

    List<EntityGroup> selectedEntityGroups =
        selectedEntityGroups(underlay, selectedIdsPerEntityGroup);

    List<EntityFilter> entityFilters = new ArrayList<>();
    selectedEntityGroups.forEach(
        entityGroup -> {
          List<Literal> selectedIds = selectedIdsPerEntityGroup.get(entityGroup);
          if (selectedIds == null) {
            selectedIds = new ArrayList<>();
          }

          switch (entityGroup.getType()) {
            case CRITERIA_OCCURRENCE:
              entityFilters.add(
                  buildPrimaryWithCriteriaFilter(
                      underlay,
                      (CriteriaOccurrence) entityGroup,
                      selectedIds,
                      criteriaSelectionData,
                      modifiersSelectionData));
              break;
            case GROUP_ITEMS:
              entityFilters.add(
                  buildGroupItemsFilter(
                      underlay, (GroupItems) entityGroup, selectedIds, modifiersSelectionData));
              break;
            default:
              throw new SystemException("Unsupported entity group type: " + entityGroup.getType());
          }
        });

    return entityFilters.size() == 1
        ? entityFilters.get(0)
        : new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, entityFilters);
  }

  @Override
  public List<EntityOutput> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    String criteriaSelectionData = selectionData.get(0).getPluginData();

    Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup =
        selectedIdsPerEntityGroup(underlay, criteriaSelectionData);
    List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());

    List<EntityGroup> selectedEntityGroups =
        selectedEntityGroups(underlay, selectedIdsPerEntityGroup);

    if (selectedIdsPerEntityGroup.isEmpty() && modifiersSelectionData.isEmpty()) {
      // Empty selection data = output all occurrence entities with null filters.
      // Use the list of all possible entity groups in the config.
      Set<Entity> outputEntities = new HashSet<>();
      selectedEntityGroups.forEach(
          entityGroup -> {
            switch (entityGroup.getType()) {
              case CRITERIA_OCCURRENCE:
                CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;
                outputEntities.addAll(criteriaOccurrence.getOccurrenceEntities());
                break;
              case GROUP_ITEMS:
                GroupItems groupItems = (GroupItems) entityGroup;
                outputEntities.add(
                    groupItems.getItemsEntity().isPrimary()
                        ? groupItems.getGroupEntity()
                        : groupItems.getItemsEntity());
                break;
              default:
                throw new SystemException(
                    "Unsupported entity group type: " + entityGroup.getType());
            }
          });
      return outputEntities.stream().map(EntityOutput::unfiltered).collect(Collectors.toList());
    } else {
      // Check that there are no group by modifiers.
      Optional<Pair<CFUnhintedValue.UnhintedValue, DTUnhintedValue.UnhintedValue>>
          groupByModifierConfigAndData =
              GroupByCountSchemaUtils.getModifier(criteriaSelector, modifiersSelectionData);
      if (groupByModifierConfigAndData.isPresent()) {
        throw new InvalidQueryException("Group by modifiers are not supported for data features");
      }

      // We want to build filters per entity group, not per selected id.
      ValueDataOuterClass.ValueData valueData = valueData(criteriaSelectionData);

      Map<Entity, List<EntityFilter>> filtersPerEntity = new HashMap<>();
      selectedEntityGroups.forEach(
          entityGroup -> {
            Map<Entity, List<EntityFilter>> filtersForSingleEntityGroup;
            List<Literal> selectedIds = selectedIdsPerEntityGroup.get(entityGroup);
            if (selectedIds == null) {
              selectedIds = new ArrayList<>();
            }

            switch (entityGroup.getType()) {
              case CRITERIA_OCCURRENCE:
                CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;
                filtersForSingleEntityGroup =
                    EntityGroupFilterUtils.addOccurrenceFiltersForDataFeature(
                        underlay, criteriaOccurrence, selectedIds);
                EntityGroupFilterUtils.buildAllModifierFilters(
                    underlay,
                    criteriaOccurrence.getOccurrenceEntities(),
                    criteriaSelector,
                    valueData,
                    modifiersSelectionData,
                    filtersForSingleEntityGroup);
                break;
              case GROUP_ITEMS:
                GroupItems groupItems = (GroupItems) entityGroup;
                Entity notPrimaryEntity =
                    groupItems.getGroupEntity().isPrimary()
                        ? groupItems.getItemsEntity()
                        : groupItems.getGroupEntity();

                filtersForSingleEntityGroup = new HashMap<>();
                EntityFilter idSubFilter =
                    EntityGroupFilterUtils.buildIdSubFilter(
                        underlay, notPrimaryEntity, selectedIds);
                if (idSubFilter == null) {
                  filtersForSingleEntityGroup.put(notPrimaryEntity, new ArrayList<>());
                } else {
                  filtersForSingleEntityGroup.put(
                      notPrimaryEntity, new ArrayList<>(List.of(idSubFilter)));
                }
                EntityGroupFilterUtils.buildAllModifierFilters(
                    underlay,
                    List.of(notPrimaryEntity),
                    criteriaSelector,
                    valueData,
                    modifiersSelectionData,
                    filtersForSingleEntityGroup);
                break;
              default:
                throw new SystemException(
                    "Unsupported entity group type: " + entityGroup.getType());
            }

            List<EntityOutput> entityOutputsForSingleEntityGroup =
                EntityGroupFilterUtils.mergeFiltersForDataFeature(
                    filtersForSingleEntityGroup, BooleanAndOrFilter.LogicalOperator.AND);
            entityOutputsForSingleEntityGroup.forEach(
                entityOutput -> {
                  List<EntityFilter> filters =
                      filtersPerEntity.getOrDefault(entityOutput.getEntity(), new ArrayList<>());
                  if (entityOutput.hasDataFeatureFilter()) {
                    filters.add(entityOutput.getDataFeatureFilter());
                  } else {
                    filters = new ArrayList<>();
                  }
                  filtersPerEntity.put(entityOutput.getEntity(), filters);
                });
          });

      // If there are multiple filters for a single entity, OR them together.
      return EntityGroupFilterUtils.mergeFiltersForDataFeature(
          filtersPerEntity, BooleanAndOrFilter.LogicalOperator.OR);
    }
  }

  private Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup(
      Underlay underlay, String serializedSelectionData) {
    Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup = new HashMap<>();
    Map<Literal, String> selectedIdsAndEntityGroups =
        selectedIdsAndEntityGroups(serializedSelectionData);
    selectedIdsAndEntityGroups.forEach(
        (key, entityGroupId) -> {
          EntityGroup entityGroup = underlay.getEntityGroup(entityGroupId);
          List<Literal> selectedIds =
              selectedIdsPerEntityGroup.containsKey(entityGroup)
                  ? selectedIdsPerEntityGroup.get(entityGroup)
                  : new ArrayList<>();
          selectedIds.add(key);
          selectedIdsPerEntityGroup.put(entityGroup, selectedIds);
        });

    // Sort selected IDs so they're consistent for tests rather than returning them in the original
    // selection order.
    selectedIdsPerEntityGroup.forEach(
        (entityGroup, selectedIds) -> {
          Collections.sort(selectedIds);
        });
    return selectedIdsPerEntityGroup;
  }

  // Returns a list of the union of entity groups covered by the selected items or all configured
  // entity groups if no items are selected.
  private List<EntityGroup> selectedEntityGroups(
      Underlay underlay, Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup) {
    List<EntityGroup> selectedEntityGroups;
    if (!selectedIdsPerEntityGroup.isEmpty()) {
      selectedEntityGroups = new ArrayList<>(selectedIdsPerEntityGroup.keySet());
    } else {
      selectedEntityGroups =
          entityGroupIds().stream()
              .map(
                  entityGroupId -> {
                    return underlay.getEntityGroup(entityGroupId);
                  })
              .collect(Collectors.toList());
    }

    return selectedEntityGroups.stream()
        .sorted(Comparator.comparing(EntityGroup::getName))
        .collect(Collectors.toList());
  }

  private EntityFilter buildPrimaryWithCriteriaFilter(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      List<Literal> selectedIds,
      String serializedSelectionData,
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

    // Build the instance-level modifier filters.
    ValueDataOuterClass.ValueData valueData = valueData(serializedSelectionData);
    if (valueData != null
        && !IGNORED_ATTRIBUTE_NAME_UI_USE_ONLY.equalsIgnoreCase(valueData.getAttribute())) {
      if (criteriaOccurrence.getOccurrenceEntities().size() > 1) {
        throw new InvalidQueryException(
            "Instance-level modifiers are not supported for entity groups with multiple occurrence entities: "
                + criteriaOccurrence.getName());
      }
      Entity occurrenceEntity = criteriaOccurrence.getOccurrenceEntities().get(0);

      EntityFilter attrFilter =
          AttributeSchemaUtils.buildForEntity(
              underlay,
              occurrenceEntity,
              occurrenceEntity.getAttribute(valueData.getAttribute()),
              valueData);
      List<EntityFilter> subFilters =
          subFiltersPerOccurrenceEntity.containsKey(occurrenceEntity)
              ? subFiltersPerOccurrenceEntity.get(occurrenceEntity)
              : new ArrayList<>();
      subFilters.add(attrFilter);
      subFiltersPerOccurrenceEntity.put(occurrenceEntity, subFilters);
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

  protected abstract List<String> entityGroupIds();

  protected abstract Map<Literal, String> selectedIdsAndEntityGroups(
      String serializedSelectionData);

  protected abstract ValueDataOuterClass.ValueData valueData(String serializedSelectionData);
}
