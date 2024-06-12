package bio.terra.tanagra.underlay;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.QueryRunner;
import bio.terra.tanagra.query.bigquery.BQQueryRunner;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZCriteriaSelector;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZGroupItems;
import bio.terra.tanagra.underlay.serialization.SZPrepackagedCriteria;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.underlay.uiplugin.CriteriaSelector;
import bio.terra.tanagra.underlay.uiplugin.PrepackagedCriteria;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
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
    justification =
        "Jackson object mapper writes the POJO fields during deserialization. Need to put this at the class level, because method-level does not handle internal lambdas.")
public final class Underlay {
  private final String name;
  private final String displayName;
  private @Nullable final String description;
  private final ImmutableMap<String, String> properties;
  private final QueryRunner queryRunner;
  private final ImmutableList<Entity> entities;
  private final ImmutableList<EntityGroup> entityGroups;
  private final SourceSchema sourceSchema;
  private final IndexSchema indexSchema;
  private final ClientConfig clientConfig;
  private final ImmutableList<CriteriaSelector> criteriaSelectors;
  private final ImmutableList<PrepackagedCriteria> prepackagedDataFeatures;
  private final String uiConfig;

  @SuppressWarnings("checkstyle:ParameterNumber")
  private Underlay(
      String name,
      String displayName,
      @Nullable String description,
      @Nullable Map<String, String> properties,
      QueryRunner queryRunner,
      List<Entity> entities,
      List<EntityGroup> entityGroups,
      SourceSchema sourceSchema,
      IndexSchema indexSchema,
      ClientConfig clientConfig,
      List<CriteriaSelector> criteriaSelectors,
      List<PrepackagedCriteria> prepackagedDataFeatures,
      String uiConfig) {
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.properties = properties == null ? ImmutableMap.of() : ImmutableMap.copyOf(properties);
    this.queryRunner = queryRunner;
    this.entities = ImmutableList.copyOf(entities);
    this.entityGroups = ImmutableList.copyOf(entityGroups);
    this.sourceSchema = sourceSchema;
    this.indexSchema = indexSchema;
    this.clientConfig = clientConfig;
    this.criteriaSelectors = ImmutableList.copyOf(criteriaSelectors);
    this.prepackagedDataFeatures = ImmutableList.copyOf(prepackagedDataFeatures);
    this.uiConfig = uiConfig;
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName == null || displayName.isEmpty() ? name : displayName;
  }

  public String getDescription() {
    return description;
  }

  public ImmutableMap<String, String> getProperties() {
    return properties;
  }

  public String getMetadataProperty(String key) {
    return properties.get(key);
  }

  public ImmutableList<Entity> getEntities() {
    return entities;
  }

  public Entity getEntity(String name) {
    return entities.stream()
        .filter(e -> name.equals(e.getName()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Entity not found: " + name));
  }

  public Entity getPrimaryEntity() {
    return entities.stream()
        .filter(Entity::isPrimary)
        .findFirst()
        .orElseThrow(() -> new SystemException("No primary entity defined"));
  }

  public ImmutableList<EntityGroup> getEntityGroups() {
    return entityGroups;
  }

  public EntityGroup getEntityGroup(String name) {
    return entityGroups.stream()
        .filter(eg -> name.equals(eg.getName()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Entity group not found: " + name));
  }

  public Pair<EntityGroup, Relationship> getRelationship(Entity entity1, Entity entity2) {
    for (EntityGroup entityGroup : entityGroups) {
      Optional<Relationship> relationship =
          entityGroup.getRelationships().stream()
              .filter(r -> r.matchesEntities(entity1, entity2))
              .min(
                  Comparator.comparing(
                      r -> r.getEntityA().getName() + ',' + r.getEntityB().getName()));
      if (relationship.isPresent()) {
        return Pair.of(entityGroup, relationship.get());
      }
    }
    throw new NotFoundException(
        "Relationship not found for entities: " + entity1.getName() + ", " + entity2.getName());
  }

  public QueryRunner getQueryRunner() {
    return queryRunner;
  }

  public SourceSchema getSourceSchema() {
    return sourceSchema;
  }

  public IndexSchema getIndexSchema() {
    return indexSchema;
  }

  public ClientConfig getClientConfig() {
    return clientConfig;
  }

  public ImmutableList<CriteriaSelector> getCriteriaSelectors() {
    return criteriaSelectors;
  }

  public CriteriaSelector getCriteriaSelector(String name) {
    return criteriaSelectors.stream()
        .filter(cs -> name.equals(cs.getName()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Criteria selector not found: " + name));
  }

  public ImmutableList<PrepackagedCriteria> getPrepackagedDataFeatures() {
    return prepackagedDataFeatures;
  }

  public PrepackagedCriteria getPrepackagedDataFeature(String name) {
    return prepackagedDataFeatures.stream()
        .filter(pdf -> name.equals(pdf.getName()))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Prepackaged data feature not found: " + name));
  }

  public String getUiConfig() {
    return uiConfig;
  }

  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  public static Underlay fromConfig(
      SZBigQuery szBigQuery, SZUnderlay szUnderlay, ConfigReader configReader) {
    // Build the source and index table schemas.
    configReader.setUnderlay(szUnderlay.name);
    configReader.setSqlSubstitutions(szBigQuery.sourceData.sqlSubstitutions);
    SourceSchema sourceSchema = SourceSchema.fromConfig(szBigQuery, szUnderlay, configReader);
    IndexSchema indexSchema = IndexSchema.fromConfig(szBigQuery, szUnderlay, configReader);

    // Build the entities.
    Set<SZEntity> szEntities = new HashSet<>();
    List<Entity> entities =
        szUnderlay.entities.stream()
            .map(
                entityPath -> {
                  SZEntity szEntity = configReader.readEntity(entityPath);
                  szEntities.add(szEntity);
                  return fromConfigEntity(szEntity, szUnderlay.primaryEntity);
                })
            .collect(Collectors.toList());

    // Check for the primary entity.
    if (entities.stream()
        .filter(entity -> entity.getName().equals(szUnderlay.primaryEntity))
        .findAny()
        .isEmpty()) {
      throw new InvalidConfigException("Primary entity not found: " + szUnderlay.primaryEntity);
    }

    // Build the entity groups.
    Set<SZGroupItems> szGroupItemsEntityGroups = new HashSet<>();
    Set<SZCriteriaOccurrence> szCriteriaOccurrenceEntityGroups = new HashSet<>();
    List<EntityGroup> entityGroups = new ArrayList<>();
    szUnderlay.groupItemsEntityGroups.forEach(
        groupItemsPath -> {
          SZGroupItems szGroupItems = configReader.readGroupItems(groupItemsPath);
          szGroupItemsEntityGroups.add(szGroupItems);
          entityGroups.add(fromConfigGroupItems(szGroupItems, entities));
        });
    szUnderlay.criteriaOccurrenceEntityGroups.forEach(
        criteriaOccurrencePath -> {
          SZCriteriaOccurrence szCriteriaOccurrence =
              configReader.readCriteriaOccurrence(criteriaOccurrencePath);
          szCriteriaOccurrenceEntityGroups.add(szCriteriaOccurrence);
          entityGroups.add(
              fromConfigCriteriaOccurrence(
                  szCriteriaOccurrence, entities, szUnderlay.primaryEntity));
        });

    // Build the query executor.
    BQQueryRunner queryRunner =
        new BQQueryRunner(szBigQuery.queryProjectId, szBigQuery.dataLocation);

    // Build the criteria selectors.
    List<SZCriteriaSelector> szCriteriaSelectors = new ArrayList<>();
    List<CriteriaSelector> criteriaSelectors = new ArrayList<>();
    if (szUnderlay.criteriaSelectors != null) {
      szUnderlay.criteriaSelectors.forEach(
          criteriaSelectorPath -> {
            SZCriteriaSelector szCriteriaSelector =
                configReader.readCriteriaSelector(criteriaSelectorPath);
            CriteriaSelector criteriaSelector =
                fromConfigCriteriaSelector(szCriteriaSelector, criteriaSelectorPath, configReader);

            // Update the szCriteriaSelector with the contents of the plugin config files.
            szCriteriaSelector.pluginConfig = criteriaSelector.getPluginConfig();
            szCriteriaSelector.modifiers.forEach(
                modifier ->
                    modifier.pluginConfig =
                        criteriaSelector.getModifier(modifier.name).getPluginConfig());

            szCriteriaSelectors.add(szCriteriaSelector);
            criteriaSelectors.add(criteriaSelector);
          });
    }

    // Build the prepackaged data features.
    Set<SZPrepackagedCriteria> szPrepackagedDataFeatures = new HashSet<>();
    List<PrepackagedCriteria> prepackagedDataFeatures = new ArrayList<>();
    if (szUnderlay.prepackagedDataFeatures != null) {
      szUnderlay.prepackagedDataFeatures.forEach(
          prepackagedCriteriaPath -> {
            SZPrepackagedCriteria szPrepackagedCriteria =
                configReader.readPrepackagedCriteria(prepackagedCriteriaPath);
            PrepackagedCriteria prepackagedCriteria =
                fromConfigPrepackagedCriteria(
                    szPrepackagedCriteria, prepackagedCriteriaPath, configReader);

            // Update the szPrepackagedCriteria with the contents of the plugin data files.
            if (prepackagedCriteria.hasSelectionData()) {
              szPrepackagedCriteria.pluginData =
                  prepackagedCriteria.getSelectionData().getPluginData();
            }

            szPrepackagedDataFeatures.add(szPrepackagedCriteria);
            prepackagedDataFeatures.add(prepackagedCriteria);
          });
    }

    // Read the UI config.
    String uiConfig = configReader.readUIConfig(szUnderlay.uiConfigFile);

    return new Underlay(
        szUnderlay.name,
        szUnderlay.metadata.displayName,
        szUnderlay.metadata.description,
        szUnderlay.metadata.properties,
        queryRunner,
        entities,
        entityGroups,
        sourceSchema,
        indexSchema,
        new ClientConfig(
            szUnderlay,
            szEntities,
            szGroupItemsEntityGroups,
            szCriteriaOccurrenceEntityGroups,
            szCriteriaSelectors,
            szPrepackagedDataFeatures),
        criteriaSelectors,
        prepackagedDataFeatures,
        uiConfig);
  }

  @VisibleForTesting
  public static Entity fromConfigEntity(SZEntity szEntity, String primaryEntityName) {
    // Build the attributes.
    List<Attribute> attributes =
        szEntity.attributes.stream()
            .map(
                szAttribute -> {
                  Attribute.SourceQuery sourceQuery =
                      szAttribute.sourceQuery == null
                          ? new Attribute.SourceQuery(
                              szAttribute.valueFieldName == null
                                  ? szAttribute.name
                                  : szAttribute.valueFieldName,
                              null,
                              null,
                              null)
                          : new Attribute.SourceQuery(
                              szAttribute.sourceQuery.valueFieldName == null
                                  ? (szAttribute.valueFieldName == null
                                      ? szAttribute.name
                                      : szAttribute.valueFieldName)
                                  : szAttribute.sourceQuery.valueFieldName,
                              szAttribute.sourceQuery.displayFieldTable,
                              szAttribute.sourceQuery.displayFieldName,
                              szAttribute.sourceQuery.displayFieldTableJoinFieldName);
                  return new Attribute(
                      szAttribute.name,
                      ConfigReader.deserializeDataType(szAttribute.dataType),
                      szAttribute.displayFieldName != null,
                      szAttribute.name.equals(szEntity.idAttribute),
                      szAttribute.runtimeSqlFunctionWrapper,
                      ConfigReader.deserializeDataType(szAttribute.runtimeDataType),
                      szAttribute.isComputeDisplayHint,
                      szAttribute.isSuppressedForExport,
                      szEntity.temporalQuery != null
                          && szAttribute.name.equals(szEntity.temporalQuery.visitDateAttribute),
                      szEntity.temporalQuery != null
                          && szAttribute.name.equals(szEntity.temporalQuery.visitIdAttribute),
                      sourceQuery);
                })
            .collect(Collectors.toList());

    List<Attribute> optimizeGroupByAttributes = new ArrayList<>();
    if (szEntity.optimizeGroupByAttributes != null) {
      optimizeGroupByAttributes =
          attributes.stream()
              .filter(attribute -> szEntity.optimizeGroupByAttributes.contains(attribute.getName()))
              .collect(Collectors.toList());
    }
    List<Attribute> optimizeTextSearchAttributes = new ArrayList<>();
    if (szEntity.textSearch != null && szEntity.textSearch.attributes != null) {
      optimizeTextSearchAttributes =
          attributes.stream()
              .filter(attribute -> szEntity.textSearch.attributes.contains(attribute.getName()))
              .collect(Collectors.toList());
    }

    // Build the hierarchies.
    List<Hierarchy> hierarchies = new ArrayList<>();
    if (szEntity.hierarchies != null) {
      hierarchies =
          szEntity.hierarchies.stream()
              .map(
                  szHierarchy ->
                      new Hierarchy(
                          szHierarchy.name,
                          szHierarchy.maxDepth,
                          szHierarchy.keepOrphanNodes,
                          szHierarchy.rootNodeIds,
                          szHierarchy.cleanHierarchyNodesWithZeroCounts))
              .collect(Collectors.toList());
    }
    return new Entity(
        szEntity.name,
        szEntity.displayName,
        szEntity.description,
        primaryEntityName.equals(szEntity.name),
        attributes,
        hierarchies,
        optimizeGroupByAttributes,
        szEntity.textSearch != null,
        optimizeTextSearchAttributes,
        szEntity.sourceQueryTableName);
  }

  private static GroupItems fromConfigGroupItems(SZGroupItems szGroupItems, List<Entity> entities) {
    // Get the entities.
    Entity groupEntity =
        entities.stream()
            .filter(entity -> entity.getName().equals(szGroupItems.groupEntity))
            .findFirst()
            .get();
    Entity itemsEntity =
        entities.stream()
            .filter(entity -> entity.getName().equals(szGroupItems.itemsEntity))
            .findFirst()
            .get();

    // Build the relationship.
    Relationship groupItemsRelationship =
        new Relationship(
            groupEntity,
            itemsEntity,
            null,
            szGroupItems.foreignKeyAttributeItemsEntity == null
                ? null
                : itemsEntity.getAttribute(szGroupItems.foreignKeyAttributeItemsEntity));
    return new GroupItems(szGroupItems.name, groupEntity, itemsEntity, groupItemsRelationship);
  }

  private static CriteriaOccurrence fromConfigCriteriaOccurrence(
      SZCriteriaOccurrence szCriteriaOccurrence, List<Entity> entities, String primaryEntityName) {
    // Get the criteria and primary entities.
    Entity criteriaEntity =
        entities.stream()
            .filter(entity -> entity.getName().equals(szCriteriaOccurrence.criteriaEntity))
            .findFirst()
            .get();
    Entity primaryEntity =
        entities.stream()
            .filter(entity -> entity.getName().equals(primaryEntityName))
            .findFirst()
            .get();

    List<Entity> occurrenceEntities = new ArrayList<>();
    Map<String, Relationship> occurrenceCriteriaRelationships = new HashMap<>();
    Map<String, Relationship> occurrencePrimaryRelationships = new HashMap<>();
    Map<String, Set<String>> occurrenceAttributesWithInstanceLevelHints = new HashMap<>();
    szCriteriaOccurrence.occurrenceEntities.forEach(
        szOccurrenceEntity -> {
          // Get the occurrence entity.
          Entity occurrenceEntity =
              entities.stream()
                  .filter(entity -> entity.getName().equals(szOccurrenceEntity.occurrenceEntity))
                  .findFirst()
                  .get();
          occurrenceEntities.add(occurrenceEntity);

          // Build the occurrence-criteria relationship.
          Relationship occurrenceCriteriaRelationship =
              new Relationship(
                  occurrenceEntity,
                  criteriaEntity,
                  szOccurrenceEntity.criteriaRelationship.foreignKeyAttributeOccurrenceEntity
                          == null
                      ? null
                      : occurrenceEntity.getAttribute(
                          szOccurrenceEntity
                              .criteriaRelationship
                              .foreignKeyAttributeOccurrenceEntity),
                  null);
          occurrenceCriteriaRelationships.put(
              occurrenceEntity.getName(), occurrenceCriteriaRelationship);

          // Build the occurrence-primary relationship.
          Relationship occurrencePrimaryRelationship =
              new Relationship(
                  occurrenceEntity,
                  primaryEntity,
                  szOccurrenceEntity.primaryRelationship.foreignKeyAttributeOccurrenceEntity == null
                      ? null
                      : occurrenceEntity.getAttribute(
                          szOccurrenceEntity
                              .primaryRelationship
                              .foreignKeyAttributeOccurrenceEntity),
                  null);
          occurrencePrimaryRelationships.put(
              occurrenceEntity.getName(), occurrencePrimaryRelationship);

          // Get the attributes with instance-level hints.
          occurrenceAttributesWithInstanceLevelHints.put(
              occurrenceEntity.getName(), szOccurrenceEntity.attributesWithInstanceLevelHints);
        });

    // Build the primary-criteria relationship.
    Relationship primaryCriteriaRelationship =
        new Relationship(primaryEntity, criteriaEntity, null, null);

    return new CriteriaOccurrence(
        szCriteriaOccurrence.name,
        criteriaEntity,
        occurrenceEntities,
        primaryEntity,
        occurrenceCriteriaRelationships,
        occurrencePrimaryRelationships,
        primaryCriteriaRelationship,
        occurrenceAttributesWithInstanceLevelHints);
  }

  @VisibleForTesting
  public static CriteriaSelector fromConfigCriteriaSelector(
      SZCriteriaSelector szCriteriaSelector,
      String criteriaSelectorPath,
      ConfigReader configReader) {
    // Read in the plugin config file to a string.
    String pluginConfig = szCriteriaSelector.pluginConfig;
    if (szCriteriaSelector.pluginConfigFile != null) {
      pluginConfig =
          configReader.readCriteriaSelectorPluginConfig(
              criteriaSelectorPath, szCriteriaSelector.pluginConfigFile);
    }

    // Deserialize the modifiers.
    List<CriteriaSelector.Modifier> modifiers = new ArrayList<>();
    if (szCriteriaSelector.modifiers != null) {
      szCriteriaSelector.modifiers.forEach(
          szModifier -> {
            String modifierPluginConfig = szModifier.pluginConfig;
            if (szModifier.pluginConfigFile != null && !szModifier.pluginConfigFile.isEmpty()) {
              modifierPluginConfig =
                  configReader.readCriteriaSelectorPluginConfig(
                      criteriaSelectorPath, szModifier.pluginConfigFile);
            }
            modifiers.add(
                new CriteriaSelector.Modifier(
                    szModifier.name, szModifier.plugin, modifierPluginConfig));
          });
    }

    return new CriteriaSelector(
        szCriteriaSelector.name,
        szCriteriaSelector.isEnabledForCohorts,
        szCriteriaSelector.isEnabledForDataFeatureSets,
        szCriteriaSelector.filterBuilder,
        szCriteriaSelector.plugin,
        pluginConfig,
        modifiers);
  }

  @VisibleForTesting
  public static PrepackagedCriteria fromConfigPrepackagedCriteria(
      SZPrepackagedCriteria szPrepackagedCriteria,
      String prepackagedCriteriaPath,
      ConfigReader configReader) {
    // Read in the plugin config files.
    if (szPrepackagedCriteria.pluginDataFile != null
        && !szPrepackagedCriteria.pluginDataFile.isEmpty()) {
      szPrepackagedCriteria.pluginData =
          configReader.readPrepackagedCriteriaPluginConfig(
              prepackagedCriteriaPath, szPrepackagedCriteria.pluginDataFile);
    }
    return new PrepackagedCriteria(
        szPrepackagedCriteria.name,
        szPrepackagedCriteria.criteriaSelector,
        szPrepackagedCriteria.pluginData);
  }
}
