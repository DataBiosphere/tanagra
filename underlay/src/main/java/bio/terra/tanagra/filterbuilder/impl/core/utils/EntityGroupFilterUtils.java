package bio.terra.tanagra.filterbuilder.impl.core.utils;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public final class EntityGroupFilterUtils {
  private EntityGroupFilterUtils() {}

  public static EntityFilter buildIdSubFilter(
      Underlay underlay, Entity criteriaEntity, List<Literal> criteriaIds) {
    // Build the criteria sub-filter.
    if (criteriaEntity.hasHierarchies()) {
      // Use a has ancestor filter.
      return new HierarchyHasAncestorFilter(
          underlay,
          criteriaEntity,
          criteriaEntity.getHierarchy(Hierarchy.DEFAULT_NAME),
          criteriaIds);
    } else {
      // Use an attribute filter on the id.
      return criteriaIds.size() > 1
          ? new AttributeFilter(
              underlay,
              criteriaEntity,
              criteriaEntity.getIdAttribute(),
              NaryOperator.IN,
              criteriaIds)
          : new AttributeFilter(
              underlay,
              criteriaEntity,
              criteriaEntity.getIdAttribute(),
              BinaryOperator.EQUALS,
              criteriaIds.get(0));
    }
  }

  public static Map<Entity, List<EntityFilter>> buildAttributeModifierFilters(
      Underlay underlay,
      CriteriaSelector criteriaSelector,
      List<SelectionData> modifiersSelectionData,
      List<Entity> occurrenceEntities) {
    Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity = new HashMap<>();
    AttributeSchemaUtils.getModifiers(criteriaSelector, modifiersSelectionData).stream()
        .forEach(
            configAndData -> {
              CFPlaceholder.Placeholder modifierConfig = configAndData.getLeft();
              DTAttribute.Attribute modifierData = configAndData.getRight();

              // Add a separate filter for each occurrence entity.
              occurrenceEntities.stream()
                  .forEach(
                      occurrenceEntity -> {
                        List<EntityFilter> subFilters =
                            subFiltersPerOccurrenceEntity.containsKey(occurrenceEntity)
                                ? subFiltersPerOccurrenceEntity.get(occurrenceEntity)
                                : new ArrayList<>();
                        subFilters.add(
                            AttributeSchemaUtils.buildForEntity(
                                underlay,
                                occurrenceEntity,
                                occurrenceEntity.getAttribute(modifierConfig.getAttribute()),
                                modifierData));
                        subFiltersPerOccurrenceEntity.put(occurrenceEntity, subFilters);
                      });
            });
    return subFiltersPerOccurrenceEntity;
  }

  public static EntityFilter buildGroupItemsFilter(
      Underlay underlay,
      CriteriaSelector criteriaSelector,
      GroupItems groupItems,
      List<EntityFilter> filtersOnNotPrimaryEntity,
      List<SelectionData> modifiersSelectionData) {
    Entity notPrimaryEntity =
        groupItems.getGroupEntity().isPrimary()
            ? groupItems.getItemsEntity()
            : groupItems.getGroupEntity();

    // Compile a list of all sub filters on the non-primary entity, which includes any passed in and
    // any attribute modifiers.
    List<EntityFilter> allFiltersNotPrimaryEntity = new ArrayList<>();
    allFiltersNotPrimaryEntity.addAll(filtersOnNotPrimaryEntity);

    // Build the attribute modifier filters for the non-primary entity.
    Map<Entity, List<EntityFilter>> attributeModifierFilters =
        EntityGroupFilterUtils.buildAttributeModifierFilters(
            underlay, criteriaSelector, modifiersSelectionData, List.of(notPrimaryEntity));
    if (attributeModifierFilters.containsKey(notPrimaryEntity)) {
      allFiltersNotPrimaryEntity.addAll(attributeModifierFilters.get(notPrimaryEntity));
    }

    // If there's more than one filter on the non-primary entity, AND them together.
    EntityFilter notPrimarySubFilter;
    if (allFiltersNotPrimaryEntity.isEmpty()) {
      notPrimarySubFilter = null;
    } else if (allFiltersNotPrimaryEntity.size() == 1) {
      notPrimarySubFilter = allFiltersNotPrimaryEntity.get(0);
    } else {
      notPrimarySubFilter =
          new BooleanAndOrFilter(
              BooleanAndOrFilter.LogicalOperator.AND, allFiltersNotPrimaryEntity);
    }

    Optional<Pair<CFPlaceholder.Placeholder, DTUnhintedValue.UnhintedValue>>
        groupByModifierConfigAndData =
            GroupByCountSchemaUtils.getModifier(criteriaSelector, modifiersSelectionData);
    if (groupByModifierConfigAndData.isEmpty()) {
      if (groupItems.getGroupEntity().isPrimary()) {
        // e.g. vitals, person=group / height=items
        return new GroupHasItemsFilter(underlay, groupItems, notPrimarySubFilter, null, null, null);
      } else {
        // e.g. genotyping, genotyping=group / person=items
        return new ItemInGroupFilter(underlay, groupItems, notPrimarySubFilter, null, null, null);
      }
    }

    // Build the group by filter information.
    Map<Entity, List<Attribute>> groupByAttributesPerOccurrenceEntity =
        GroupByCountSchemaUtils.getGroupByAttributesPerOccurrenceEntity(
            underlay, groupByModifierConfigAndData);
    List<Attribute> groupByAttributes =
        groupByAttributesPerOccurrenceEntity.containsKey(notPrimaryEntity)
            ? groupByAttributesPerOccurrenceEntity.get(notPrimaryEntity)
            : new ArrayList<>();
    if (groupByAttributes.size() > 1) {
      // TODO: Support multiple attributes.
      throw new InvalidConfigException(
          "More than one group by attribute is not yet supported for GroupItems entity groups.");
    }
    DTUnhintedValue.UnhintedValue groupByModifierData =
        groupByModifierConfigAndData.get().getRight();
    if (groupItems.getGroupEntity().isPrimary()) {
      return new GroupHasItemsFilter(
          underlay,
          groupItems,
          notPrimarySubFilter,
          groupByAttributes.size() == 1 ? groupByAttributes.get(0) : null,
          GroupByCountSchemaUtils.toBinaryOperator(groupByModifierData.getOperator()),
          (int) groupByModifierData.getMin());
    } else {
      return new ItemInGroupFilter(
          underlay,
          groupItems,
          notPrimarySubFilter,
          groupByAttributes.size() == 1 ? groupByAttributes.get(0) : null,
          GroupByCountSchemaUtils.toBinaryOperator(groupByModifierData.getOperator()),
          (int) groupByModifierData.getMin());
    }
  }

  public static void addOccurrenceFiltersForDataFeature(
      Underlay underlay,
      CriteriaOccurrence criteriaOccurrence,
      List<Literal> criteriaIds,
      Map<Entity, List<EntityFilter>> filtersPerEntity) {
    EntityFilter criteriaSubFilterCO =
        EntityGroupFilterUtils.buildIdSubFilter(
            underlay, criteriaOccurrence.getCriteriaEntity(), criteriaIds);
    criteriaOccurrence.getOccurrenceEntities().stream()
        .sorted(Comparator.comparing(occurrenceEntity -> occurrenceEntity.getName()))
        .forEach(
            occurrenceEntity -> {
              List<EntityFilter> occurrenceEntityFilters =
                  filtersPerEntity.containsKey(occurrenceEntity)
                      ? filtersPerEntity.get(occurrenceEntity)
                      : new ArrayList<>();
              if (!criteriaIds.isEmpty()) {
                occurrenceEntityFilters.add(
                    new OccurrenceForPrimaryFilter(
                        underlay, criteriaOccurrence, occurrenceEntity, null, criteriaSubFilterCO));
              }
              filtersPerEntity.put(occurrenceEntity, occurrenceEntityFilters);
            });
  }

  public static List<EntityOutput> mergeFiltersForDataFeature(
      Map<Entity, List<EntityFilter>> filtersPerEntity,
      BooleanAndOrFilter.LogicalOperator logicalOperator) {
    // If there are multiple filters for a single entity, OR them together.
    return filtersPerEntity.entrySet().stream()
        .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
        .map(
            entry -> {
              Entity entity = entry.getKey();
              List<EntityFilter> entityFilters = entry.getValue();
              if (entityFilters.isEmpty()) {
                return EntityOutput.unfiltered(entity);
              } else if (entityFilters.size() == 1) {
                return EntityOutput.filtered(entity, entityFilters.get(0));
              } else {
                return EntityOutput.filtered(
                    entity, new BooleanAndOrFilter(logicalOperator, entityFilters));
              }
            })
        .collect(Collectors.toList());
  }
}
