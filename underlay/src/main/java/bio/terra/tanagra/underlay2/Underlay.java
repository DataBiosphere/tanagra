package bio.terra.tanagra.underlay2;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.query.bigquery.BigQueryExecutor;
import bio.terra.tanagra.underlay2.entitymodel.Attribute;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay2.entitymodel.Relationship;
import bio.terra.tanagra.underlay2.entitymodel.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay2.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay2.entitymodel.entitygroup.GroupItems;
import bio.terra.tanagra.underlay2.serialization.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

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

  private Underlay(
      String name,
      String displayName,
      @Nullable String description,
      @Nullable Map<String, String> properties,
      QueryExecutor queryExecutor,
      List<Entity> entities,
      List<EntityGroup> entityGroups,
      SourceSchema sourceSchema,
      IndexSchema indexSchema) {
    this.name = name;
    this.displayName = displayName;
    this.description = description;
    this.properties = properties == null ? ImmutableMap.of() : ImmutableMap.copyOf(properties);
    this.queryExecutor = queryExecutor;
    this.entities = ImmutableList.copyOf(entities);
    this.entityGroups = ImmutableList.copyOf(entityGroups);
    this.sourceSchema = sourceSchema;
    this.indexSchema = indexSchema;
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
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

  public static Underlay fromConfig(SZBigQuery szBigQuery, SZUnderlay szUnderlay) {
    // Build the source and index table schemas.
    ConfigReader configReader = new ConfigReader(szUnderlay.name);
    SourceSchema sourceSchema = SourceSchema.fromConfig(szBigQuery, szUnderlay, configReader);
    IndexSchema indexSchema = IndexSchema.fromConfig(szBigQuery, szUnderlay, configReader);

    // Build the entities.
    List<Entity> entities =
        szUnderlay.entities.stream()
            .map(
                entityName ->
                    fromConfigEntity(configReader.readEntity(entityName), szUnderlay.primaryEntity))
            .collect(Collectors.toList());

    // Build the entity groups.
    List<EntityGroup> entityGroups = new ArrayList<>();
    szUnderlay.groupItemsEntityGroups.stream()
        .forEach(
            groupItemsName ->
                entityGroups.add(
                    fromConfigGroupItems(configReader.readGroupItems(groupItemsName), entities)));
    szUnderlay.criteriaOccurrenceEntityGroups.stream()
        .forEach(
            criteriaOccurrenceName ->
                entityGroups.add(
                    fromConfigCriteriaOccurrence(
                        configReader.readCriteriaOccurrence(criteriaOccurrenceName),
                        entities,
                        szUnderlay.primaryEntity)));

    // Build the query executor.
    BigQueryExecutor queryExecutor =
        new BigQueryExecutor(szBigQuery.queryProjectId, szBigQuery.dataLocation);

    return new Underlay(
        szUnderlay.name,
        szUnderlay.metadata.displayName,
        szUnderlay.metadata.description,
        szUnderlay.metadata.properties,
        queryExecutor,
        entities,
        entityGroups,
        sourceSchema,
        indexSchema);
  }

  private static Entity fromConfigEntity(SZEntity szEntity, String primaryEntityName) {
    // Build the attributes.
    List<Attribute> attributes =
        szEntity.attributes.stream()
            .map(
                szAttribute ->
                    new Attribute(
                        szAttribute.name,
                        szAttribute.dataType,
                        szAttribute.valueFieldName != null,
                        szAttribute.name.equals(szEntity.idAttribute),
                        szAttribute.isComputeDisplayHint))
            .collect(Collectors.toList());

    List<Attribute> optimizeGroupByAttributes =
        attributes.stream()
            .filter(attribute -> szEntity.optimizeGroupByAttributes.contains(attribute.getName()))
            .collect(Collectors.toList());
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
                          szHierarchy.name, szHierarchy.maxDepth, szHierarchy.keepOrphanNodes))
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
        new Relationship(criteriaEntity, primaryEntity, null, null);

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
