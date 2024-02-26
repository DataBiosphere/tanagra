package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJson;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.GroupHasItemsFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.ItemInGroupFilter;
import bio.terra.tanagra.api.filter.OccurrenceForPrimaryFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.filterbuilder.EntityOutput;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.core.utils.AttributeSchemaUtils;
import bio.terra.tanagra.proto.criteriaselector.configschema.CFPlaceholder;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTAttribute;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTEntityGroup;
import bio.terra.tanagra.proto.criteriaselector.dataschema.DTUnhintedValue;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZCorePlugin;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
                  CriteriaOccurrence criteriaOccurrence = (CriteriaOccurrence) entityGroup;
                  EntityFilter criteriaSubFilterCO =
                      buildCriteriaSubFilter(
                          underlay, criteriaOccurrence.getCriteriaEntity(), selectedIds);
                  criteriaOccurrence.getOccurrenceEntities().stream()
                      .forEach(
                          occurrenceEntity -> {
                            List<EntityFilter> occurrenceEntityFilters =
                                filtersPerEntity.containsKey(occurrenceEntity)
                                    ? filtersPerEntity.get(occurrenceEntity)
                                    : new ArrayList<>();
                            if (!selectedIds.isEmpty()) {
                              occurrenceEntityFilters.add(
                                  new OccurrenceForPrimaryFilter(
                                      underlay,
                                      criteriaOccurrence,
                                      occurrenceEntity,
                                      null,
                                      criteriaSubFilterCO));
                            }
                            filtersPerEntity.put(occurrenceEntity, occurrenceEntityFilters);
                          });
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
                        buildCriteriaSubFilter(underlay, notPrimaryEntity, selectedIds));
                  }
                  filtersPerEntity.put(notPrimaryEntity, notPrimaryEntityFilters);
                  break;
                default:
                  throw new SystemException(
                      "Unsupported entity group type: " + entityGroup.getType());
              }
            });

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
                    entity,
                    new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, entityFilters));
              }
            })
        .collect(Collectors.toList());
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
        buildCriteriaSubFilter(underlay, criteriaOccurrence.getCriteriaEntity(), selectedIds);

    // Build the attribute modifier filters.
    Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity = new HashMap<>();
    modifiersSelectionData.stream()
        .filter(
            modifierSelectionData ->
                SZCorePlugin.ATTRIBUTE
                    .getIdInConfig()
                    .equals(
                        criteriaSelector
                            .getModifier(modifierSelectionData.getModifierName())
                            .getPlugin()))
        .forEach(
            modifierSelectionData -> {
              CriteriaSelector.Modifier modifierDefn =
                  criteriaSelector.getModifier(modifierSelectionData.getModifierName());
              CFPlaceholder.Placeholder modifierConfig =
                  AttributeSchemaUtils.deserializeConfig(modifierDefn.getPluginConfig());
              DTAttribute.Attribute modifierData =
                  AttributeSchemaUtils.deserializeData(modifierSelectionData.getPluginData());

              // Add a separate filter for each occurrence entity.
              criteriaOccurrence.getOccurrenceEntities().stream()
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

    Optional<SelectionData> groupByCountSelectionData =
        modifiersSelectionData.stream()
            .filter(
                modifierSelectionData ->
                    SZCorePlugin.UNHINTED_VALUE
                        .getIdInConfig()
                        .equals(
                            criteriaSelector
                                .getModifier(modifierSelectionData.getModifierName())
                                .getPlugin()))
            .findFirst();
    if (groupByCountSelectionData.isEmpty()) {
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
    CFPlaceholder.Placeholder groupByModifierConfig =
        deserializeGroupByCountConfig(
            criteriaSelector
                .getModifier(groupByCountSelectionData.get().getModifierName())
                .getPluginConfig());
    Map<Entity, List<Attribute>> groupByAttributesPerOccurrenceEntity = new HashMap<>();
    if (!groupByModifierConfig.getGroupByAttributesPerOccurrenceEntityMap().isEmpty()) {
      groupByModifierConfig.getGroupByAttributesPerOccurrenceEntityMap().entrySet().stream()
          .forEach(
              entry -> {
                Entity occurrenceEntity = underlay.getEntity(entry.getKey());
                List<Attribute> groupByAttributes =
                    entry.getValue().getAttributeList().stream()
                        .map(attrName -> occurrenceEntity.getAttribute(attrName))
                        .collect(Collectors.toList());
                groupByAttributesPerOccurrenceEntity.put(occurrenceEntity, groupByAttributes);
              });
    }
    DTUnhintedValue.UnhintedValue groupByModifierData =
        deserializeGroupByCountData(groupByCountSelectionData.get().getPluginData());

    return new PrimaryWithCriteriaFilter(
        underlay,
        criteriaOccurrence,
        criteriaSubFilter,
        subFiltersPerOccurrenceEntity,
        groupByAttributesPerOccurrenceEntity,
        toBinaryOperator(groupByModifierData.getOperator()),
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
    List<EntityFilter> subFiltersGroupEntity = new ArrayList<>();
    if (!selectedIds.isEmpty()) {
      subFiltersGroupEntity.add(buildCriteriaSubFilter(underlay, notPrimaryEntity, selectedIds));
    }

    // Build the attribute modifier filters for the non-primary entity.
    modifiersSelectionData.stream()
        .filter(
            modifierSelectionData ->
                SZCorePlugin.ATTRIBUTE
                    .getIdInConfig()
                    .equals(
                        criteriaSelector
                            .getModifier(modifierSelectionData.getModifierName())
                            .getPlugin()))
        .forEach(
            modifierSelectionData -> {
              CriteriaSelector.Modifier modifierDefn =
                  criteriaSelector.getModifier(modifierSelectionData.getModifierName());
              CFPlaceholder.Placeholder modifierConfig =
                  AttributeSchemaUtils.deserializeConfig(modifierDefn.getPluginConfig());
              DTAttribute.Attribute modifierData =
                  AttributeSchemaUtils.deserializeData(modifierSelectionData.getPluginData());
              subFiltersGroupEntity.add(
                  AttributeSchemaUtils.buildForEntity(
                      underlay, notPrimaryEntity, modifierConfig, modifierData));
            });

    // If there's more than one filter on the non-primary entity, AND them together.
    EntityFilter notPrimarySubFilter;
    if (subFiltersGroupEntity.isEmpty()) {
      notPrimarySubFilter = null;
    } else if (subFiltersGroupEntity.size() == 1) {
      notPrimarySubFilter = subFiltersGroupEntity.get(0);
    } else {
      notPrimarySubFilter =
          new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.AND, subFiltersGroupEntity);
    }

    Optional<SelectionData> groupByCountSelectionData =
        modifiersSelectionData.stream()
            .filter(
                modifierSelectionData ->
                    SZCorePlugin.UNHINTED_VALUE
                        .getIdInConfig()
                        .equals(
                            criteriaSelector
                                .getModifier(modifierSelectionData.getModifierName())
                                .getPlugin()))
            .findFirst();
    if (groupByCountSelectionData.isEmpty()) {
      if (groupItems.getGroupEntity().isPrimary()) {
        // e.g. vitals, person=group / height=items
        return new GroupHasItemsFilter(underlay, groupItems, notPrimarySubFilter, null, null, null);
      } else {
        // e.g. genotyping, genotyping=group / person=items
        return new ItemInGroupFilter(underlay, groupItems, notPrimarySubFilter, null, null, null);
      }
    }

    // Build the group by filter information.
    CFPlaceholder.Placeholder groupByModifierConfig =
        deserializeGroupByCountConfig(
            criteriaSelector
                .getModifier(groupByCountSelectionData.get().getModifierName())
                .getPluginConfig());
    List<Attribute> groupByAttributes;
    if (groupByModifierConfig
        .getGroupByAttributesPerOccurrenceEntityMap()
        .containsKey(notPrimaryEntity.getName())) {
      groupByAttributes =
          groupByModifierConfig.getGroupByAttributesPerOccurrenceEntityMap()
              .get(notPrimaryEntity.getName()).getAttributeList().stream()
              .map(attrName -> notPrimaryEntity.getAttribute(attrName))
              .collect(Collectors.toList());
    } else {
      groupByAttributes = new ArrayList<>();
    }
    if (groupByAttributes.size() > 1) {
      // TODO: Support multiple attributes.
      throw new InvalidConfigException(
          "More than one group by attribute is not yet supported for GroupItems entity groups.");
    }
    DTUnhintedValue.UnhintedValue groupByModifierData =
        deserializeGroupByCountData(groupByCountSelectionData.get().getPluginData());

    if (groupItems.getGroupEntity().isPrimary()) {
      return new GroupHasItemsFilter(
          underlay,
          groupItems,
          notPrimarySubFilter,
          groupByAttributes.size() == 1 ? groupByAttributes.get(0) : null,
          toBinaryOperator(groupByModifierData.getOperator()),
          (int) groupByModifierData.getMin());
    } else {
      return new ItemInGroupFilter(
          underlay,
          groupItems,
          notPrimarySubFilter,
          groupByAttributes.size() == 1 ? groupByAttributes.get(0) : null,
          toBinaryOperator(groupByModifierData.getOperator()),
          (int) groupByModifierData.getMin());
    }
  }

  private EntityFilter buildCriteriaSubFilter(
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

  public static CFPlaceholder.Placeholder deserializeGroupByCountConfig(String serialized) {
    return deserializeFromJson(serialized, CFPlaceholder.Placeholder.newBuilder()).build();
  }

  public static DTUnhintedValue.UnhintedValue deserializeGroupByCountData(String serialized) {
    return deserializeFromJson(serialized, DTUnhintedValue.UnhintedValue.newBuilder()).build();
  }

  private static BinaryOperator toBinaryOperator(
      DTUnhintedValue.UnhintedValue.ComparisonOperator comparisonOperator) {
    switch (comparisonOperator) {
      case COMPARISON_OPERATOR_EQUAL:
        return BinaryOperator.EQUALS;
      case COMPARISON_OPERATOR_LESS_THAN_EQUAL:
        return BinaryOperator.LESS_THAN_OR_EQUAL;
      case COMPARISON_OPERATOR_GREATER_THAN_EQUAL:
        return BinaryOperator.GREATER_THAN_OR_EQUAL;
      case COMPARISON_OPERATOR_BETWEEN:
      default:
        throw new SystemException(
            "Unsupported unhinted-value comparison operator: " + comparisonOperator);
    }
  }
}
