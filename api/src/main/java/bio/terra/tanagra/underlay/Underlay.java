package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Underlay {
  private static final String UI_CONFIG_DIRECTORY_NAME = "ui";

  private final String name;
  private final Map<String, DataPointer> dataPointers;
  private final Map<String, Entity> entities;
  private final String primaryEntityName;
  private final Map<String, EntityGroup> entityGroups;
  private final String uiConfig;

  private Underlay(
      String name,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName,
      Map<String, EntityGroup> entityGroups,
      String uiConfig) {
    this.name = name;
    this.dataPointers = dataPointers;
    this.entities = entities;
    this.primaryEntityName = primaryEntityName;
    this.entityGroups = entityGroups;
    this.uiConfig = uiConfig;
  }

  public static Underlay fromJSON(String underlayFileName) throws IOException {
    // read in the top-level underlay file
    Path underlayFilePath = FileIO.getInputParentDir().resolve(underlayFileName);
    UFUnderlay serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileIO.getGetFileInputStreamFunction().apply(underlayFilePath), UFUnderlay.class);

    // deserialize data pointers
    if (serialized.getDataPointers() == null || serialized.getDataPointers().size() == 0) {
      throw new InvalidConfigException("No DataPointer defined");
    }
    Map<String, DataPointer> dataPointers = new HashMap<>();
    serialized
        .getDataPointers()
        .forEach(dps -> dataPointers.put(dps.getName(), dps.deserializeToInternal()));

    // deserialize entities
    if (serialized.getEntities() == null || serialized.getEntities().size() == 0) {
      throw new InvalidConfigException("No Entity defined");
    }
    Map<String, Entity> entities = new HashMap<>();
    for (String entityFile : serialized.getEntities()) {
      Entity entity = Entity.fromJSON(entityFile, dataPointers);
      entities.put(entity.getName(), entity);
    }

    String primaryEntity = serialized.getPrimaryEntity();
    if (primaryEntity == null || primaryEntity.isEmpty()) {
      throw new InvalidConfigException("No primary Entity defined");
    }
    if (!entities.containsKey(primaryEntity)) {
      throw new InvalidConfigException("Primary Entity not found in the set of Entities");
    }

    // deserialize entity groups
    Map<String, EntityGroup> entityGroups = new HashMap<>();
    if (serialized.getEntityGroups() != null) {
      for (String entityGroupFile : serialized.getEntityGroups()) {
        EntityGroup entityGroup =
            EntityGroup.fromJSON(entityGroupFile, dataPointers, entities, primaryEntity);
        entityGroups.put(entityGroup.getName(), entityGroup);
      }
    }

    String uiConfig = serialized.getUiConfig();
    if (serialized.getUiConfigFile() != null) {
      // read in UI config from file
      if (serialized.getUiConfigFile() != null) {
        Path uiConfigFilePath =
            FileIO.getInputParentDir()
                .resolve(UI_CONFIG_DIRECTORY_NAME)
                .resolve(serialized.getUiConfigFile());
        uiConfig =
            FileUtils.readStringFromFile(
                FileIO.getGetFileInputStreamFunction().apply(uiConfigFilePath));
      }
    }

    return new Underlay(
        serialized.getName(), dataPointers, entities, primaryEntity, entityGroups, uiConfig);
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

  public Entity getEntity(String name) {
    if (!entities.containsKey(name)) {
      throw new SystemException("Entity not found: " + name);
    }
    return entities.get(name);
  }

  public Map<String, EntityGroup> getEntityGroups() {
    return Collections.unmodifiableMap(entityGroups);
  }

  public EntityGroup getEntityGroup(String name) {
    if (!entityGroups.containsKey(name)) {
      throw new SystemException("Entity group not found: " + name);
    }
    return entityGroups.get(name);
  }

  public String getUIConfig() {
    return uiConfig;
  }
}
