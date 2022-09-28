package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.IndexingJob;
import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitygroup.GroupItems;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    // read in entity group file
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

  public abstract Type getType();

  public abstract Map<String, Entity> getEntities();

  public abstract List<WorkflowCommand> getIndexingCommands();

  public abstract List<IndexingJob> getIndexingJobs();

  public String getName() {
    return name;
  }

  public Map<String, Relationship> getRelationships() {
    return Collections.unmodifiableMap(relationships);
  }

  public Map<String, AuxiliaryData> getAuxiliaryData() {
    return Collections.unmodifiableMap(auxiliaryData);
  }

  public EntityGroupMapping getSourceDataMapping() {
    return sourceDataMapping;
  }

  public EntityGroupMapping getIndexDataMapping() {
    return indexDataMapping;
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
