package bio.terra.tanagra.filterbuilder.impl.core;

import static bio.terra.tanagra.utils.ProtobufUtils.deserializeFromJson;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.impl.utils.AttributeSchemaUtils;
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
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.SelectionData;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.NotImplementedException;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "The config and data objects are deserialized by Jackson.")
public class EntityGroupFilterBuilder extends FilterBuilder {
  private static final String ATTRIBUTE_MODIFIER_PLUGIN = "core/attribute";
  private static final String GROUP_BY_MODIFIER_PLUGIN = "core/attribute";

  public EntityGroupFilterBuilder(CriteriaSelector criteriaSelector) {
    super(criteriaSelector);
  }

  @Override
  public EntityFilter buildForCohort(Underlay underlay, List<SelectionData> selectionData) {
    DTEntityGroup.EntityGroup entityGroupSelectionData =
        deserializeData(selectionData.get(0).getPluginData());
    List<SelectionData> modifiersSelectionData = selectionData.subList(0, selectionData.size());

    // We want to build one filter per entity group, not one filter per selected id.
    Map<EntityGroup, List<Literal>> selectedIdsPerEntityGroup = new HashMap<>();
    for (DTEntityGroup.EntityGroup.Selection selectedId :
        entityGroupSelectionData.getSelectedList()) {
      EntityGroup entityGroup = underlay.getEntityGroup(selectedId.getEntityGroup());
      List<Literal> selectedIds =
          selectedIdsPerEntityGroup.containsKey(entityGroup)
              ? selectedIdsPerEntityGroup.get(entityGroup)
              : new ArrayList<>();
      selectedIds.add(Literal.forInt64(selectedId.getKey().getInt64Key()));
      selectedIdsPerEntityGroup.put(entityGroup, selectedIds);
    }

    List<EntityFilter> entityFilters = new ArrayList<>();
    selectedIdsPerEntityGroup.entrySet().stream()
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
                  throw new NotImplementedException("Group items entity groups not supported yet");
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
  public Map<Entity, EntityFilter> buildForDataFeature(
      Underlay underlay, List<SelectionData> selectionData) {
    return null; // TODO
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
                ATTRIBUTE_MODIFIER_PLUGIN.equals(modifierSelectionData.getPlugin()))
        .forEach(
            modifierSelectionData -> {
              CriteriaSelector.Modifier modifierDefn =
                  criteriaSelector.getModifier(modifierSelectionData.getPlugin());
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
                    GROUP_BY_MODIFIER_PLUGIN.equals(modifierSelectionData.getPlugin()))
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
                .getModifier(groupByCountSelectionData.get().getPlugin())
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
  public EntityFilter buildForCohort(Underlay underlay, SelectionData selectionData) {
    throw new UnsupportedOperationException(
        "Entity group filter builder expects list of selection data.");
  }

  @Override
  protected Map<Entity, EntityFilter> buildForDataFeature(
      Underlay underlay, SelectionData selectionData) {
    throw new UnsupportedOperationException(
        "Entity group filter builder expects list of selection data.");
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
