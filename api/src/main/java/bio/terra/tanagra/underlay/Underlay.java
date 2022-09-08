package bio.terra.tanagra.underlay;

import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Underlay {
  private final String name;
  private final Map<String, DataPointer> dataPointers;
  private final Map<String, Entity> entities;
  private final String primaryEntityName;
  private final Map<String, EntityGroup> entityGroups;

  private Underlay(
      String name,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName,
      Map<String, EntityGroup> entityGroups) {
    this.name = name;
    this.dataPointers = dataPointers;
    this.entities = entities;
    this.primaryEntityName = primaryEntityName;
    this.entityGroups = entityGroups;
  }

  public static Underlay fromJSON(Path underlayFilePath) throws IOException {
    // read in the top-level underlay file
    UFUnderlay serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileIO.getGetFileInputStreamFunction().apply(underlayFilePath), UFUnderlay.class);

    // deserialize data pointers
    if (serialized.getDataPointers() == null || serialized.getDataPointers().size() == 0) {
      throw new IllegalArgumentException("No DataPointer defined");
    }
    Map<String, DataPointer> dataPointers = new HashMap<>();
    serialized
        .getDataPointers()
        .forEach(dps -> dataPointers.put(dps.getName(), dps.deserializeToInternal()));

    // entity and entity group file paths are relative to the underlay file path
    Path parentDir = underlayFilePath.getParent();

    // deserialize entities
    if (serialized.getEntities() == null || serialized.getEntities().size() == 0) {
      throw new IllegalArgumentException("No Entity defined");
    }
    Map<String, Entity> entities = new HashMap<>();
    for (String entityFile : serialized.getEntities()) {
      Entity entity = Entity.fromJSON(parentDir.resolve(entityFile), dataPointers);
      entities.put(entity.getName(), entity);
    }

    String primaryEntity = serialized.getPrimaryEntity();
    if (primaryEntity == null || primaryEntity.isEmpty()) {
      throw new IllegalArgumentException("No primary Entity defined");
    }
    if (!entities.containsKey(primaryEntity)) {
      throw new IllegalArgumentException("Primary Entity not found in the set of Entities");
    }

    // deserialize entity groups
    Map<String, EntityGroup> entityGroups = new HashMap<>();
    if (serialized.getEntityGroups() != null) {
      for (String entityGroupFile : serialized.getEntityGroups()) {
        EntityGroup entityGroup =
            EntityGroup.fromJSON(
                parentDir.resolve(entityGroupFile), dataPointers, entities, primaryEntity);
        entityGroups.put(entityGroup.getName(), entityGroup);
      }
    }

    return new Underlay(serialized.getName(), dataPointers, entities, primaryEntity, entityGroups);
  }

  public List<WorkflowCommand> getIndexingCommands() {
    List<WorkflowCommand> cmds = new ArrayList<>();
    for (Entity entity : entities.values()) {
      cmds.addAll(entity.getIndexingCommands());
    }
    for (EntityGroup entityGroup : entityGroups.values()) {
      cmds.addAll(entityGroup.getIndexingCommands());
    }
    return cmds;
  }

  public String getName() {
    return name;
  }

  public Map<String, DataPointer> getDataPointers() {
    return Collections.unmodifiableMap(dataPointers);
  }

  public Map<String, Entity> getEntities() {
    return Collections.unmodifiableMap(entities);
  }

  public Entity getPrimaryEntity() {
    return entities.get(primaryEntityName);
  }

  public Map<String, EntityGroup> getEntityGroups() {
    return Collections.unmodifiableMap(entityGroups);
  }
}
