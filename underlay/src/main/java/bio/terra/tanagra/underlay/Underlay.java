package bio.terra.tanagra.underlay;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.query.bigquery.BigQueryExecutor;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.serialization.SZCriteriaOccurrence;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import bio.terra.tanagra.underlay.serialization.SZGroupItems;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@SuppressFBWarnings(
    value = "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
    justification =
        "Jackson object mapper writes the POJO fields during deserialization. Need to put this at the class level, because method-level does not handle internal lambdas.")
public final class Underlay {
  private final String name;
  private final String displayName;
  private @Nullable final String description;
  private final ImmutableMap<String, String> properties;
  private final QueryExecutor queryExecutor;
  private final ImmutableList<Entity> entities;
  private final ImmutableList<EntityGroup> entityGroups;
  private final SourceSchema sourceSchema;
  private final IndexSchema indexSchema;
  private final DataMappingSerialization dataMappingSerialization;
  private final String uiConfig;

  @SuppressWarnings("checkstyle:ParameterNumber")
  private Underlay(
      String name,
      String displayName,
      @Nullable String description,
      @Nullable Map<String, String> properties,
      QueryExecutor queryExecutor,
      List<Entity> entities,
      List<EntityGroup> entityGroups,
      SourceSchema sourceSchema,
      IndexSchema indexSchema,
      DataMappingSerialization dataMappingSerialization,
      String uiConfig) {
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.properties = properties == null ? ImmutableMap.of() : ImmutableMap.copyOf(properties);
    this.queryExecutor = queryExecutor;
    this.entities = ImmutableList.copyOf(entities);
    this.entityGroups = ImmutableList.copyOf(entityGroups);
    this.sourceSchema = sourceSchema;
    this.indexSchema = indexSchema;
    this.dataMappingSerialization = dataMappingSerialization;
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
        .filter(e -> e.isPrimary())
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

  public QueryExecutor getQueryExecutor() {
    return queryExecutor;
  }

  public SourceSchema getSourceSchema() {
    return sourceSchema;
  }

  public IndexSchema getIndexSchema() {
    return indexSchema;
  }

  public DataMappingSerialization getDataMappingSerialization() {
    return dataMappingSerialization;
  }

  public String getUiConfig() {
    return uiConfig;
  }

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

    // Build the entity groups.
    Set<SZGroupItems> szGroupItemsEntityGroups = new HashSet<>();
    Set<SZCriteriaOccurrence> szCriteriaOccurrenceEntityGroups = new HashSet<>();
    List<EntityGroup> entityGroups = new ArrayList<>();
    szUnderlay.groupItemsEntityGroups.stream()
        .forEach(
            groupItemsPath -> {
              SZGroupItems szGroupItems = configReader.readGroupItems(groupItemsPath);
              szGroupItemsEntityGroups.add(szGroupItems);
              entityGroups.add(fromConfigGroupItems(szGroupItems, entities));
            });
    szUnderlay.criteriaOccurrenceEntityGroups.stream()
        .forEach(
            criteriaOccurrencePath -> {
              SZCriteriaOccurrence szCriteriaOccurrence =
                  configReader.readCriteriaOccurrence(criteriaOccurrencePath);
              szCriteriaOccurrenceEntityGroups.add(szCriteriaOccurrence);
              entityGroups.add(
                  fromConfigCriteriaOccurrence(
                      szCriteriaOccurrence, entities, szUnderlay.primaryEntity));
            });

    // Build the query executor.
    BigQueryExecutor queryExecutor =
        new BigQueryExecutor(szBigQuery.queryProjectId, szBigQuery.dataLocation);

    // Read the UI config.
    String uiConfig = configReader.readUIConfig(szUnderlay.uiConfigFile);

    return new Underlay(
        szUnderlay.name,
        szUnderlay.metadata.displayName,
        szUnderlay.metadata.description,
        szUnderlay.metadata.properties,
        queryExecutor,
        entities,
        entityGroups,
        sourceSchema,
        indexSchema,
        new DataMappingSerialization(
            szUnderlay, szEntities, szGroupItemsEntityGroups, szCriteriaOccurrenceEntityGroups),
        uiConfig);
  }

  @VisibleForTesting
  public static Entity fromConfigEntity(SZEntity szEntity, String primaryEntityName) {
    // Build the attributes.
    List<Attribute> attributes =
        szEntity.attributes.stream()
            .map(
                szAttribute ->
                    new Attribute(
                        szAttribute.name,
                        ConfigReader.deserializeDataType(szAttribute.dataType),
                        szAttribute.displayFieldName != null,
                        szAttribute.name.equals(szEntity.idAttribute),
                        szAttribute.runtimeSqlFunctionWrapper,
                        ConfigReader.deserializeDataType(szAttribute.runtimeDataType),
                        szAttribute.isComputeDisplayHint))
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
                          szHierarchy.rootNodeIds))
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
        optimizeTextSearchAttributes);
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
    szCriteriaOccurrence.occurrenceEntities.stream()
        .forEach(
            szOccurrenceEntity -> {
              // Get the occurrence entity.
              Entity occurrenceEntity =
                  entities.stream()
                      .filter(
                          entity -> entity.getName().equals(szOccurrenceEntity.occurrenceEntity))
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
                      szOccurrenceEntity.primaryRelationship.foreignKeyAttributeOccurrenceEntity
                              == null
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
}
