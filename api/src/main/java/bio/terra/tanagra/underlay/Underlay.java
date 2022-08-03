package bio.terra.tanagra.underlay;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

  public static Underlay fromJSON(
      String underlayFilePath, Function<String, InputStream> getFileInputStreamFunction)
      throws IOException {
    // read in the top-level underlay file
    UFUnderlay serialized =
        JacksonMapper.readFileIntoJavaObject(
            getFileInputStreamFunction.apply(underlayFilePath), UFUnderlay.class);

    // deserialize data pointers
    if (serialized.getDataPointers() == null || serialized.getDataPointers().size() == 0) {
      throw new IllegalArgumentException("No DataPointer defined");
    }
    Map<String, DataPointer> dataPointers = new HashMap<>();
    serialized
        .getDataPointers()
        .forEach(dps -> dataPointers.put(dps.getName(), dps.deserializeToInternal()));

    // read in entities
    if (serialized.getEntities() == null || serialized.getEntities().size() == 0) {
      throw new IllegalArgumentException("No Entity defined");
    }
    Map<String, Entity> entities = new HashMap<>();
    for (String entityFile : serialized.getEntities()) {
      Entity entity = Entity.fromJSON(entityFile, getFileInputStreamFunction, dataPointers);
      entities.put(entity.getName(), entity);
    }

    if (serialized.getPrimaryEntity() == null || serialized.getPrimaryEntity().isEmpty()) {
      throw new IllegalArgumentException("No primary Entity defined");
    }
    if (!entities.containsKey(serialized.getPrimaryEntity())) {
      throw new IllegalArgumentException("Primary Entity not found in the set of Entities");
    }

    return new Underlay(
        serialized.getName(), dataPointers, entities, serialized.getPrimaryEntity());
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
