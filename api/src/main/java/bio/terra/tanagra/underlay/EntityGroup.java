package bio.terra.tanagra.underlay;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitygroup.OneToMany;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class EntityGroup {
  /** Enum for the types of entity groups supported by Tanagra. */
  public enum Type {
    ONE_TO_MANY,
    CRITERIA_OCCURRENCE
  }

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
      Path entityGroupFilePath,
      Function<Path, InputStream> getFileInputStreamFunction,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName)
      throws IOException {
    // read in entity group file
    UFEntityGroup serialized =
        JacksonMapper.readFileIntoJavaObject(
            getFileInputStreamFunction.apply(entityGroupFilePath), UFEntityGroup.class);
    if (serialized.getEntities().size() == 0) {
      throw new IllegalArgumentException("There are no entities defined");
    }
    switch (serialized.getType()) {
      case ONE_TO_MANY:
        return OneToMany.fromSerialized(serialized, dataPointers, entities);
      case CRITERIA_OCCURRENCE:
        return CriteriaOccurrence.fromSerialized(
            serialized, dataPointers, entities, primaryEntityName);
      default:
        throw new RemoteException("Unknown entity group type: " + serialized.getType());
    }
  }

  protected static Entity deserializeEntity(
      UFEntityGroup serialized, String entityKey, Map<String, Entity> entities) {
    String entityName = serialized.getEntities().get(entityKey);
    if (entityName == null || entityName.isEmpty()) {
      throw new IllegalArgumentException(entityKey + " entity is undefined in entity group");
    }
    Entity entity = entities.get(entityName);
    if (entity == null) {
      throw new IllegalArgumentException(
          entityKey + " entity not found in set of entities: " + entityName);
    }
    return entity;
  }

  public abstract Type getType();

  public abstract Map<String, Entity> getEntities();

  public abstract List<WorkflowCommand> getIndexingCommands();

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
