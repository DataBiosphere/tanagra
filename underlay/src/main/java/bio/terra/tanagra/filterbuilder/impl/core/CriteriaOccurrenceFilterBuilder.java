package bio.terra.tanagra.filterbuilder.impl.core;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.filter.HierarchyHasAncestorFilter;
import bio.terra.tanagra.api.filter.PrimaryWithCriteriaFilter;
import bio.terra.tanagra.api.filter.RelationshipFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.filterbuilder.FilterBuilder;
import bio.terra.tanagra.filterbuilder.SelectionData;
import bio.terra.tanagra.filterbuilder.impl.utils.AttributeSchemaUtils;
import bio.terra.tanagra.filterbuilder.schema.attribute.PSAttributeData;
import bio.terra.tanagra.filterbuilder.schema.entitygroup.PSEntityGroupConfig;
import bio.terra.tanagra.filterbuilder.schema.entitygroup.PSEntityGroupData;
import bio.terra.tanagra.filterbuilder.schema.groupbycount.PSGroupByCountData;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.utils.JacksonMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification = "The config and data objects are deserialized by Jackson.")
public class CriteriaOccurrenceFilterBuilder extends FilterBuilder {
  public CriteriaOccurrenceFilterBuilder(Underlay underlay, String configSerialized) {
    super(underlay, configSerialized);
  }

  @Override
  public EntityFilter buildForCohort(List<SelectionData> selectionData) {
    PSEntityGroupConfig entityGroupConfig = deserializeConfig();
    PSEntityGroupData entityGroupSelectionData =
        deserializeData(selectionData.get(0).getSerialized());
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup(entityGroupSelectionData.entityGroup);

    // Build the criteria sub-filter.
    EntityFilter criteriaSubFilter =
        buildCriteriaSubFilter(
            criteriaOccurrence.getCriteriaEntity(), entityGroupSelectionData.keys);

    // Build the attribute modifier filters.
    Map<String, PSEntityGroupConfig.AttributeModifier> attributeModifiers = new HashMap<>();
    entityGroupConfig.attributeModifiers.stream()
        .forEach(
            attributeModifier -> attributeModifiers.put(attributeModifier.name, attributeModifier));

    Map<Entity, List<EntityFilter>> subFiltersPerOccurrenceEntity = new HashMap<>();
    selectionData.stream()
        .forEach(
            sd -> {
              PSEntityGroupConfig.AttributeModifier attributeModifier =
                  attributeModifiers.get(sd.getPluginName());
              if (attributeModifier == null) {
                return;
              }
              PSAttributeData attributeModifierData =
                  AttributeSchemaUtils.deserializeData(sd.getSerialized());

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
                                underlay,
                                occurrenceEntity,
                                attributeModifier.pluginConfig,
                                attributeModifierData));
                        subFiltersPerOccurrenceEntity.put(occurrenceEntity, subFilters);
                      });
            });

    String groupByModifierName =
        entityGroupConfig.groupByModifier == null ? null : entityGroupConfig.groupByModifier.name;
    Optional<SelectionData> groupByCountSelectionData =
        selectionData.stream()
            .filter(sd -> sd.getPluginName().equals(groupByModifierName))
            .findFirst();
    if (entityGroupConfig.groupByModifier == null || groupByCountSelectionData.isEmpty()) {
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
    Map<Entity, List<Attribute>> groupByAttributesPerOccurrenceEntity = new HashMap<>();
    if (!entityGroupConfig.groupByModifier.pluginConfig.attributes.isEmpty()) {
      entityGroupConfig.groupByModifier.pluginConfig.attributes.entrySet().stream()
          .forEach(
              entry -> {
                Entity occurrenceEntity = underlay.getEntity(entry.getKey());
                List<Attribute> groupByAttributes =
                    entry.getValue().stream()
                        .map(attrName -> occurrenceEntity.getAttribute(attrName))
                        .collect(Collectors.toList());
                groupByAttributesPerOccurrenceEntity.put(occurrenceEntity, groupByAttributes);
              });
    }
    PSGroupByCountData groupByCountData =
        deserializeGroupByCountData(groupByCountSelectionData.get().getSerialized());

    return new PrimaryWithCriteriaFilter(
        underlay,
        criteriaOccurrence,
        criteriaSubFilter,
        subFiltersPerOccurrenceEntity,
        groupByAttributesPerOccurrenceEntity,
        groupByCountData.operator,
        groupByCountData.value);
  }

  @Override
  public Map<Entity, EntityFilter> buildForDataFeature(List<SelectionData> selectionData) {
    if (selectionData.size() > 1) {
      throw new UnsupportedOperationException("Modifiers are not yet supported for data features");
    }

    PSEntityGroupData entityGroupSelectionData =
        deserializeData(selectionData.get(0).getSerialized());
    CriteriaOccurrence criteriaOccurrence =
        (CriteriaOccurrence) underlay.getEntityGroup(entityGroupSelectionData.entityGroup);

    // Build the criteria sub-filter.
    EntityFilter criteriaSubFilter =
        buildCriteriaSubFilter(
            criteriaOccurrence.getCriteriaEntity(), entityGroupSelectionData.keys);

    // Build a filter for each occurrence entity.
    Map<Entity, EntityFilter> occurrenceFilters = new HashMap<>();
    criteriaOccurrence.getOccurrenceEntities().stream()
        .forEach(
            occurrenceEntity ->
                occurrenceFilters.put(
                    occurrenceEntity,
                    // TODO: Replace this RelationshipFilter with a new OccurrenceForCriteriaFilter.
                    new RelationshipFilter(
                        underlay,
                        criteriaOccurrence,
                        occurrenceEntity,
                        criteriaOccurrence.getOccurrenceCriteriaRelationship(
                            occurrenceEntity.getName()),
                        criteriaSubFilter,
                        null,
                        null,
                        null)));
    return occurrenceFilters;
  }

  private EntityFilter buildCriteriaSubFilter(Entity criteriaEntity, List<Literal> criteriaIds) {
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
  public EntityFilter buildForCohort(SelectionData selectionData) {
    throw new UnsupportedOperationException(
        "Entity group filter builder expects list of selection data.");
  }

  @Override
  protected Map<Entity, EntityFilter> buildForDataFeature(SelectionData selectionData) {
    throw new UnsupportedOperationException(
        "Entity group filter builder expects list of selection data.");
  }

  @Override
  public PSEntityGroupConfig deserializeConfig() {
    return JacksonMapper.deserializeJavaObject(
        configSerialized,
        PSEntityGroupConfig.class,
        (jpEx) -> new InvalidConfigException("Error deserializing criteria selector config", jpEx));
  }

  @Override
  public PSEntityGroupData deserializeData(String serialized) {
    return JacksonMapper.deserializeJavaObject(
        serialized,
        PSEntityGroupData.class,
        (jpEx) -> new InvalidConfigException("Error deserializing criteria selector config", jpEx));
  }

  private PSGroupByCountData deserializeGroupByCountData(String serialized) {
    return JacksonMapper.deserializeJavaObject(
        serialized,
        PSGroupByCountData.class,
        (jpEx) -> new InvalidConfigException("Error deserializing group by config", jpEx));
  }
}
