package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.job.WriteRelationshipIdPairs;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitygroup.GroupItems;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class EntityGroup {
  /** Enum for the types of entity groups supported by Tanagra. */
  public enum Type {
    GROUP_ITEMS,
    CRITERIA_OCCURRENCE
  }

  public static final String ENTITY_GROUP_DIRECTORY_NAME = "entitygroup";

  protected String name;
  protected Map<String, Relationship> relationships;
  protected EntityGroupMapping sourceDataMapping;
  protected EntityGroupMapping indexDataMapping;

  protected EntityGroup(Builder builder) {
    this.name = builder.name;
    this.relationships = builder.relationships;
    this.sourceDataMapping = builder.sourceDataMapping;
    this.indexDataMapping = builder.indexDataMapping;
  }

  public static EntityGroup fromJSON(
      String entityGroupFileName,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName)
      throws IOException {
    // Read in entity group file.
    Path entityGroupFilePath =
        FileIO.getInputParentDir()
            .resolve(ENTITY_GROUP_DIRECTORY_NAME)
            .resolve(entityGroupFileName);
    UFEntityGroup serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileIO.getGetFileInputStreamFunction().apply(entityGroupFilePath), UFEntityGroup.class);
    if (serialized.getEntities().size() == 0) {
      throw new InvalidConfigException("There are no entities defined");
    }
    switch (serialized.getType()) {
      case GROUP_ITEMS:
        return GroupItems.fromSerialized(serialized, dataPointers, entities);
      case CRITERIA_OCCURRENCE:
        return CriteriaOccurrence.fromSerialized(
            serialized, dataPointers, entities, primaryEntityName);
      default:
        throw new InvalidConfigException("Unknown entity group type: " + serialized.getType());
    }
  }

  protected static Entity getDeserializedEntity(
      UFEntityGroup serialized, String entityKey, Map<String, Entity> entities) {
    String entityName = serialized.getEntities().get(entityKey);
    if (entityName == null || entityName.isEmpty()) {
      throw new InvalidConfigException(entityKey + " entity is undefined in entity group");
    }
    Entity entity = entities.get(entityName);
    if (entity == null) {
      throw new InvalidConfigException(
          entityKey + " entity not found in set of entities: " + entityName);
    }
    return entity;
  }

  protected static void deserializeRelationshipMappings(
      UFEntityGroup serialized, EntityGroup entityGroup) {
    // Source+index relationship mappings.
    if (serialized.getSourceDataMapping().getRelationshipMappings() == null) {
      return;
    }
    for (Relationship relationship : entityGroup.getRelationships().values()) {
      UFRelationshipMapping serializedSourceMapping =
          serialized.getSourceDataMapping().getRelationshipMappings().get(relationship.getName());
      DataPointer sourceDataPointer =
          entityGroup.getMapping(Underlay.MappingType.SOURCE).getDataPointer();
      if (serializedSourceMapping == null) {
        throw new InvalidConfigException(
            "Relationship mapping for " + relationship.getName() + " is undefined");
      }
      RelationshipMapping sourceMapping =
          RelationshipMapping.fromSerialized(serializedSourceMapping, sourceDataPointer);

      DataPointer indexDataPointer =
          entityGroup.getMapping(Underlay.MappingType.INDEX).getDataPointer();
      Map<String, UFRelationshipMapping> indexRelationshipMappings =
          serialized.getIndexDataMapping().getRelationshipMappings();
      RelationshipMapping indexMapping =
          indexRelationshipMappings == null || indexRelationshipMappings.isEmpty()
              ? RelationshipMapping.defaultIndexMapping(indexDataPointer, relationship)
              : RelationshipMapping.fromSerialized(
                  serialized
                      .getIndexDataMapping()
                      .getRelationshipMappings()
                      .get(relationship.getName()),
                  indexDataPointer);

      relationship.initialize(sourceMapping, indexMapping, entityGroup);
    }
  }

  public abstract Type getType();

  public abstract Map<String, Entity> getEntityMap();

  public List<IndexingJob> getIndexingJobs() {
    List<IndexingJob> jobs = new ArrayList<>();

    // for each relationship, write the index relationship mapping
    getRelationships().values().stream()
        .forEach(
            // TODO: If the source relationship mapping table = one of the entity tables, then just
            // populate a
            // new column on that entity table, instead of always writing a new table.
            relationship -> jobs.add(new WriteRelationshipIdPairs(relationship)));

    return jobs;
  }

  public String getName() {
    return name;
  }

  public Map<String, Relationship> getRelationships() {
    return Collections.unmodifiableMap(relationships);
  }

  public Optional<Relationship> getRelationship(Entity fromEntity, Entity toEntity) {
    for (Relationship relationship : relationships.values()) {
      if ((relationship.getEntityA().equals(fromEntity)
              && relationship.getEntityB().equals(toEntity))
          || (relationship.getEntityB().equals(fromEntity)
              && relationship.getEntityA().equals(toEntity))) {
        return Optional.of(relationship);
      }
    }
    return Optional.empty();
  }

  public EntityGroupMapping getMapping(Underlay.MappingType mappingType) {
    return Underlay.MappingType.SOURCE.equals(mappingType) ? sourceDataMapping : indexDataMapping;
  }

  protected abstract static class Builder {
    private String name;
    private Map<String, Relationship> relationships;
    private EntityGroupMapping sourceDataMapping;
    private EntityGroupMapping indexDataMapping;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder relationships(Map<String, Relationship> relationships) {
      this.relationships = relationships;
      return this;
    }

    public Builder sourceDataMapping(EntityGroupMapping sourceDataMapping) {
      this.sourceDataMapping = sourceDataMapping;
      return this;
    }

    public Builder indexDataMapping(EntityGroupMapping indexDataMapping) {
      this.indexDataMapping = indexDataMapping;
      return this;
    }

    public abstract EntityGroup build();
  }
}
