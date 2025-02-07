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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "The config and data objects are deserialized by Jackson.")
public abstract class EntityGroupFilterBuilderBase<CF, DT> extends FilterBuilder<CF, DT> {
  public EntityGroupFilterBuilderBase(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  // A SelectionGroup is the parameters that must be the same to be applied to a list of ids.
  protected static class SelectionGroup implements Comparable<SelectionGroup> {
    public String entityGroupId;
    public ValueDataOuterClass.ValueData valueData;

    public SelectionGroup(String entityGroupId, ValueDataOuterClass.ValueData valueData) {
      this.entityGroupId = entityGroupId;
      this.valueData = valueData;
    }

    public String getEntityGroupId() {
      return entityGroupId;
    }

    @Override
    public int compareTo(SelectionGroup sg) {
      if (sg == null) {
        return 1;
      } else if (sg == this) {
        return 0;
      }

      // A simple comparison for test stability.
      int result = entityGroupId.compareTo(sg.entityGroupId);
      if (result == 0) {
        if (valueData != null && sg.valueData != null) {
          result = valueData.getAttribute().compareTo(sg.valueData.getAttribute());
        } else if (valueData == null) {
          result = -1;
        } else {
          result = 1;
        }
      }
      return result;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      @SuppressWarnings("unchecked")
      SelectionGroup sg = (SelectionGroup) o;
      return entityGroupId.equals(sg.entityGroupId) && Objects.equals(valueData, sg.valueData);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entityGroupId, valueData);
    }
  }

  protected static class SelectionItem {
    public Literal id;
    public SelectionGroup group;

    public SelectionItem(Literal id, SelectionGroup group) {
      this.id = id;
      this.group = group;
    }
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    String criteriaSelectionData = selectionData.get(0).pluginData();
    List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());

    // We want to build one filter per selection group, not one filter per selected id.
    Map<SelectionGroup, List<Literal>> selectedIdsPerGroup =
        selectedIdsPerGroup(criteriaSelectionData);
    List<SelectionGroup> selectedGroups = selectedGroups(selectedIdsPerGroup);

    List<EntityFilter> entityFilters = new ArrayList<>();
    selectedGroups.forEach(
        selectionGroup -> {
          EntityGroup entityGroup = underlay.getEntityGroup(selectionGroup.entityGroupId);
          List<Literal> selectedIds = selectedIdsPerGroup.get(selectionGroup);
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
                      modifiersSelectionData,
                      selectionGroup.valueData));
              break;
            case GROUP_ITEMS:
              entityFilters.add(
                  EntityGroupFilterUtils.buildGroupItemsFilterFromIds(
                      underlay,
                      criteriaSelector,
                      (GroupItems) entityGroup,
                      selectedIds,
                      modifiersSelectionData));
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
    String criteriaSelectionData = selectionData.get(0).pluginData();
    List<SelectionData> modifiersSelectionData = selectionData.subList(1, selectionData.size());

    // We want to build one filter per selection group, not one filter per selected id.
    Map<SelectionGroup, List<Literal>> selectedIdsPerGroup =
        selectedIdsPerGroup(criteriaSelectionData);
    List<SelectionGroup> selectedGroups = selectedGroups(selectedIdsPerGroup);

    if (selectedIdsPerGroup.isEmpty() && modifiersSelectionData.isEmpty()) {
      // Empty selection data = output all occurrence entities with null filters.
      // Use the list of all possible entity groups in the config.
      Set<Entity> outputEntities = new HashSet<>();
      selectedGroups.forEach(
          selectionGroup -> {
            EntityGroup entityGroup = underlay.getEntityGroup(selectionGroup.entityGroupId);
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

      // We want to build filters per selection group, not per selected id.
      Map<Entity, List<EntityFilter>> filtersPerEntity = new HashMap<>();
      selectedGroups.forEach(
          selectionGroup -> {
            EntityGroup entityGroup = underlay.getEntityGroup(selectionGroup.entityGroupId);
            List<Literal> selectedIds = selectedIdsPerGroup.get(selectionGroup);
            if (selectedIds == null) {
              selectedIds = new ArrayList<>();
            }

            Map<Entity, List<EntityFilter>> filtersForSingleEntityGroup;
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
                    selectionGroup.valueData,
                    modifiersSelectionData,
                    filtersForSingleEntityGroup);
                break;
              case GROUP_ITEMS:
                GroupItems groupItems = (GroupItems) entityGroup;
                Entity nonPrimaryEntity =
                    groupItems.getGroupEntity().isPrimary()
                        ? groupItems.getItemsEntity()
                        : groupItems.getGroupEntity();

                filtersForSingleEntityGroup = new HashMap<>();
                EntityFilter idSubFilter =
                    EntityGroupFilterUtils.buildIdSubFilter(
                        underlay, nonPrimaryEntity, selectedIds, false);
                if (idSubFilter == null) {
                  filtersForSingleEntityGroup.put(nonPrimaryEntity, new ArrayList<>());
                } else {
                  filtersForSingleEntityGroup.put(
                      nonPrimaryEntity, new ArrayList<>(List.of(idSubFilter)));
                }
                EntityGroupFilterUtils.buildAllModifierFilters(
                    underlay,
                    List.of(nonPrimaryEntity),
                    criteriaSelector,
                    selectionGroup.valueData,
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

  private Map<SelectionGroup, List<Literal>> selectedIdsPerGroup(String serializedSelectionData) {
    Map<SelectionGroup, List<Literal>> selectedIdsPerGroup = new HashMap<>();
    List<SelectionItem> selectedIdsAndGroups = selectedIdsAndGroups(serializedSelectionData);
    selectedIdsAndGroups.forEach(
        item -> {
          List<Literal> selectedIds =
              selectedIdsPerGroup.containsKey(item.group)
                  ? selectedIdsPerGroup.get(item.group)
                  : new ArrayList<>();
          selectedIds.add(item.id);
          selectedIdsPerGroup.put(item.group, selectedIds);
        });

    // Sort selected IDs so they're consistent for tests rather than returning them in the original
    // selection order.
    selectedIdsPerGroup.forEach((selectionGroup, selectedIds) -> Collections.sort(selectedIds));
    return selectedIdsPerGroup;
  }

  // Returns a list of the union of entity groups covered by the selected items or all configured
  // entity groups if no items are selected.
  private List<SelectionGroup> selectedGroups(
      Map<SelectionGroup, List<Literal>> selectedIdsPerGroup) {
    List<SelectionGroup> selectedGroups;
    if (!selectedIdsPerGroup.isEmpty()) {
      selectedGroups = new ArrayList<>(selectedIdsPerGroup.keySet());
    } else {
      selectedGroups =
          entityGroupIds().stream()
              .map(entityGroupId -> new SelectionGroup(entityGroupId, null))
              .collect(Collectors.toList());
    }

    return selectedGroups.stream().sorted().collect(Collectors.toList());
  }

  private EntityFilter buildPrimaryWithCriteriaFilter(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      List<Literal> selectedIds,
      List<SelectionData> modifiersSelectionData,
      ValueDataOuterClass.ValueData valueData) {
    // Build the criteria sub-filter.
    EntityFilter criteriaSubFilter =
        EntityGroupFilterUtils.buildIdSubFilter(
            underlay, criteriaOccurrence.getCriteriaEntity(), selectedIds, false);

    // Build the attribute modifier filters.
    Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity =
        EntityGroupFilterUtils.buildAttributeModifierFilters(
            underlay,
            criteriaSelector,
            modifiersSelectionData,
            criteriaOccurrence.getOccurrenceEntities());

    // Build the instance-level modifier filters.
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

  protected abstract List<String> entityGroupIds();

  protected abstract List<SelectionItem> selectedIdsAndGroups(String serializedSelectionData);
}
