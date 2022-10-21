package bio.terra.tanagra.underlay;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.serialization.UFEntityGroupMapping;
import java.util.Map;

public final class EntityGroupMapping {
  private final DataPointer dataPointer;
  private EntityGroup entityGroup;

  private EntityGroupMapping(DataPointer dataPointer) {
    this.dataPointer = dataPointer;
  }

  public void initialize(EntityGroup entityGroup) {
    this.entityGroup = entityGroup;
  }

  public static EntityGroupMapping fromSerialized(
      UFEntityGroupMapping serialized, Map<String, DataPointer> dataPointers) {
    if (serialized.getDataPointer() == null || serialized.getDataPointer().isEmpty()) {
      throw new InvalidConfigException("No Data Pointer defined");
    }
    if (!dataPointers.containsKey(serialized.getDataPointer())) {
      throw new InvalidConfigException("Data Pointer not found: " + serialized.getDataPointer());
    }
    DataPointer dataPointer = dataPointers.get(serialized.getDataPointer());

    return new EntityGroupMapping(dataPointer);
  }

  public DataPointer getDataPointer() {
    return dataPointer;
  }

  public EntityGroup getEntityGroup() {
    return entityGroup;
  }
}
