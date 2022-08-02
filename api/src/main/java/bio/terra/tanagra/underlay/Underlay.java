package bio.terra.tanagra.underlay;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Underlay {
  private String name;
  private Map<String, DataPointer> dataPointers;
  private Map<String, Entity> entities;
  private String primaryEntityName;

  private Underlay(
      String name,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName) {
    this.name = name;
    this.dataPointers = dataPointers;
    this.entities = entities;
    this.primaryEntityName = primaryEntityName;
  }

  public static Underlay deserialize(UFUnderlay serialized, boolean isFromResourceFile)
      throws IOException {
    // deserialize data pointers
    if (serialized.dataPointers == null || serialized.dataPointers.size() == 0) {
      throw new IllegalArgumentException("No DataPointer defined");
    }
    Map<String, DataPointer> dataPointers = new HashMap<>();
    serialized.dataPointers.forEach(dps -> dataPointers.put(dps.name, dps.deserializeToInternal()));

    // read in entities
    if (serialized.entities == null || serialized.entities.size() == 0) {
      throw new IllegalArgumentException("No Entity defined");
    }
    Map<String, Entity> entities = new HashMap<>();
    for (String entityFile : serialized.entities) {
      InputStream entityInputStream =
          isFromResourceFile
              ? FileUtils.getResourceFileStream(entityFile)
              : new FileInputStream(Path.of(entityFile).toFile());
      UFEntity serializedEntity =
          JacksonMapper.readFileIntoJavaObject(entityInputStream, UFEntity.class);
      Entity entity = Entity.deserialize(serializedEntity, dataPointers);
      entities.put(entity.getName(), entity);
    }

    if (serialized.primaryEntity == null || serialized.primaryEntity.isEmpty()) {
      throw new IllegalArgumentException("No primary Entity defined");
    }
    if (!entities.containsKey(serialized.primaryEntity)) {
      throw new IllegalArgumentException("Primary Entity not found in the set of Entities");
    }

    return new Underlay(serialized.name, dataPointers, entities, serialized.primaryEntity);
  }

  public List<WorkflowCommand> getIndexingCommands() {
    List<WorkflowCommand> cmds = new ArrayList<>();
    for (Entity entity : entities.values()) {
      cmds.addAll(entity.getIndexingCommands());
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
}
