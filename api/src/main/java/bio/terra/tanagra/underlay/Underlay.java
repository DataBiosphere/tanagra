package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Underlay {
  private String name;
  private Map<String, DataPointer> dataPointers;
  private Map<String, Entity> entities;
  private String primaryEntityName;

  private Map<String, EntityGroup> entityGroups;

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

  public static Underlay fromJSON(String resourceFilePath) {
    // read in top-level underlay file
    UFUnderlay serialized;
    try {
      serialized = JacksonMapper.readFileIntoJavaObject(resourceFilePath, UFUnderlay.class);
    } catch (IOException ioEx) {
      throw new RuntimeException("Error deserializing Underlay from JSON", ioEx);
    }

    // deserialize data pointers
    if (serialized.dataPointers == null || serialized.dataPointers.size() == 0) {
      throw new IllegalArgumentException("No DataPointer defined");
    }
    Map<String, DataPointer> dataPointers = new HashMap<>();
    serialized.dataPointers.forEach(
        dps -> {
          System.out.println("DataPointer name: " + dps.name);
        });
    serialized.dataPointers.forEach(
        dps -> {
          dataPointers.put(dps.name, dps.deserializeToInternal());
        });

    // read in entities
    if (serialized.entities == null || serialized.entities.size() == 0) {
      throw new IllegalArgumentException("No Entity defined");
    }
    Map<String, Entity> entities = new HashMap<>();
    for (String entityFile : serialized.entities) {
      Entity entity = Entity.fromJSON(entityFile, dataPointers);
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

  public Entity getPrimaryEntity() {
    return entities.get(primaryEntityName);
  }

  public Map<String, DataPointer> getDataPointers() {
    return Collections.unmodifiableMap(dataPointers);
  }
}
