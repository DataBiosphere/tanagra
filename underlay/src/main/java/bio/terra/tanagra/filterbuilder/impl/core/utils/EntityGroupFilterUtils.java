package bio.terra.tanagra.filterbuilder.impl.core.utils;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                                underlay, occurrenceEntity, modifierConfig, modifierData));
                        subFiltersPerOccurrenceEntity.put(occurrenceEntity, subFilters);
                      });
            });
    return subFiltersPerOccurrenceEntity;
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
