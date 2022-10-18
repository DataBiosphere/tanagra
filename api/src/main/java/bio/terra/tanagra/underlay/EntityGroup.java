package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.serialization.UFAuxiliaryDataMapping;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.UFRelationshipMapping;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitygroup.GroupItems;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
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
  protected Map<String, AuxiliaryData> auxiliaryData;
  protected EntityGroupMapping sourceDataMapping;
  protected EntityGroupMapping indexDataMapping;

  protected EntityGroup(Builder builder) {
    this.name = builder.name;
    this.relationships = builder.relationships;
    this.auxiliaryData = builder.auxiliaryData;
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
    Map<String, RelationshipMapping> sourceRelationshipMappings = new HashMap<>();
    Map<String, RelationshipMapping> indexRelationshipMappings = new HashMap<>();

    Map<String, UFRelationshipMapping> serializedSourceRelationshipMappings =
        serialized.getSourceDataMapping().getRelationshipMappings() == null
            ? new HashMap<>()
            : serialized.getSourceDataMapping().getRelationshipMappings();
    for (Relationship relationship : entityGroup.getRelationships().values()) {
      if (!serializedSourceRelationshipMappings.containsKey(relationship.getName())) {
        throw new InvalidConfigException(
            "Relationship mapping for " + relationship.getName() + " is undefined");
      }
      sourceRelationshipMappings.put(
          relationship.getName(),
          RelationshipMapping.fromSerialized(
              serializedSourceRelationshipMappings.get(relationship.getName()),
              entityGroup.getMapping(Underlay.MappingType.SOURCE).getDataPointer()));
      // TODO: Copy relationship mapping from source to index dataset.
      indexRelationshipMappings.put(
          relationship.getName(),
          RelationshipMapping.fromSerialized(
              serializedSourceRelationshipMappings.get(relationship.getName()),
              entityGroup.getMapping(Underlay.MappingType.SOURCE).getDataPointer()));
    }
    serializedSourceRelationshipMappings
        .keySet()
        .forEach(
            srm -> {
              if (entityGroup.getRelationships().values().stream()
                      .filter(r -> r.getName().equals(srm))
                      .findFirst()
                  == null) {
                throw new InvalidConfigException("Unexpected relationship mapping: " + srm);
              }
            });

    entityGroup.getRelationships().values().stream()
        .forEach(
            relationship -> {
              relationship.initialize(
                  sourceRelationshipMappings.get(relationship.getName()),
                  indexRelationshipMappings.get(relationship.getName()),
                  entityGroup);
            });
  }

  protected static void deserializeAuxiliaryDataMappings(
      UFEntityGroup serialized, EntityGroup entityGroup) {
    // Source+index auxiliary data mappings.
    Map<String, AuxiliaryDataMapping> sourceMappings = new HashMap<>();
    Map<String, AuxiliaryDataMapping> indexMappings = new HashMap<>();

    DataPointer sourceDataPointer =
        entityGroup.getMapping(Underlay.MappingType.SOURCE).getDataPointer();
    DataPointer indexDataPointer =
        entityGroup.getMapping(Underlay.MappingType.INDEX).getDataPointer();

    Map<String, UFAuxiliaryDataMapping> serializedSourceMappings =
        serialized.getSourceDataMapping().getAuxiliaryDataMappings() == null
            ? new HashMap<>()
            : serialized.getSourceDataMapping().getAuxiliaryDataMappings();
    Map<String, UFAuxiliaryDataMapping> serializedIndexMappings =
        serialized.getIndexDataMapping().getAuxiliaryDataMappings() == null
            ? new HashMap<>()
            : serialized.getIndexDataMapping().getAuxiliaryDataMappings();
    for (AuxiliaryData auxiliaryData : entityGroup.getAuxiliaryData().values()) {
      if (serializedSourceMappings.containsKey(auxiliaryData.getName())) {
        sourceMappings.put(
            auxiliaryData.getName(),
            AuxiliaryDataMapping.fromSerialized(
                serializedSourceMappings.get(auxiliaryData.getName()),
                sourceDataPointer,
                auxiliaryData));
      }
      if (serializedIndexMappings.containsKey(auxiliaryData.getName())) {
        indexMappings.put(
            auxiliaryData.getName(),
            AuxiliaryDataMapping.fromSerialized(
                serializedIndexMappings.get(auxiliaryData.getName()),
                indexDataPointer,
                auxiliaryData));
      } else {
        indexMappings.put(
            auxiliaryData.getName(),
            AuxiliaryDataMapping.defaultIndexMapping(
                auxiliaryData, entityGroup.getName() + "_", indexDataPointer));
      }
    }
    sourceMappings
        .keySet()
        .forEach(
            srad -> {
              if (entityGroup.getAuxiliaryData().values().stream()
                      .filter(ad -> ad.getName().equals(srad))
                      .findFirst()
                  == null) {
                throw new InvalidConfigException("Unexpected auxiliary data mapping: " + srad);
              }
            });

    entityGroup.getAuxiliaryData().values().stream()
        .forEach(
            auxiliaryData -> {
              auxiliaryData.initialize(
                  sourceMappings.get(auxiliaryData.getName()),
                  indexMappings.get(auxiliaryData.getName()));
            });
  }

  public abstract Type getType();

  public abstract Map<String, Entity> getEntities();

  public abstract List<IndexingJob> getIndexingJobs();

  public String getName() {
    return name;
  }

  public Map<String, Relationship> getRelationships() {
    return Collections.unmodifiableMap(relationships);
  }

  public Optional<Relationship> getRelationship(Entity fromEntity, Entity toEntity) {
    for (Relationship relationship : relationships.values()) {
      if (relationship.getEntityA().equals(fromEntity)
          && relationship.getEntityB().equals(toEntity)) {
        return Optional.of(relationship);
      }
    }
    return Optional.empty();
  }

  public Map<String, AuxiliaryData> getAuxiliaryData() {
    return Collections.unmodifiableMap(auxiliaryData);
  }

  public AuxiliaryData getAuxiliaryData(String name) {
    return auxiliaryData.get(name);
  }

  public EntityGroupMapping getMapping(Underlay.MappingType mappingType) {
    return Underlay.MappingType.SOURCE.equals(mappingType) ? sourceDataMapping : indexDataMapping;
  }

  protected abstract static class Builder {
    private String name;
    private Map<String, Relationship> relationships;
    private Map<String, AuxiliaryData> auxiliaryData;
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

    public Builder auxiliaryData(Map<String, AuxiliaryData> auxiliaryData) {
      this.auxiliaryData = auxiliaryData;
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
