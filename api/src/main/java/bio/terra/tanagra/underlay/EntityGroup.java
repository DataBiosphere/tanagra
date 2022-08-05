package bio.terra.tanagra.underlay;

import bio.terra.tanagra.indexing.WorkflowCommand;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.entitygroup.UFCriteriaOccurrence;
import bio.terra.tanagra.serialization.entitygroup.UFOneToMany;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import bio.terra.tanagra.underlay.entitygroup.OneToMany;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class EntityGroup {
  /** Enum for the types of entity groups supported by Tanagra. */
  public enum Type {
    ONE_TO_MANY,
    CRITERIA_OCCURRENCE
  }

  private String name;
  private DataPointer indexDataPointer;

  protected EntityGroup(String name, DataPointer indexDataPointer) {
    this.name = name;
    this.indexDataPointer = indexDataPointer;
  }

  public static EntityGroup fromJSON(
      String entityGroupFilePath,
      Function<String, InputStream> getFileInputStreamFunction,
      Map<String, DataPointer> dataPointers,
      Map<String, Entity> entities,
      String primaryEntityName)
      throws IOException {
    // read in entity group file
    UFEntityGroup serialized =
        JacksonMapper.readFileIntoJavaObject(
            getFileInputStreamFunction.apply(entityGroupFilePath), UFEntityGroup.class);
    return serialized.deserializeToInternal(dataPointers, entities, primaryEntityName);
  }

  public abstract Type getType();

  public abstract List<WorkflowCommand> getIndexingCommands();

  public UFEntityGroup serialize() {
    switch (getType()) {
      case ONE_TO_MANY:
        return new UFOneToMany((OneToMany) this);
      case CRITERIA_OCCURRENCE:
        return new UFCriteriaOccurrence((CriteriaOccurrence) this);
      default:
        throw new RuntimeException("Unknown entity group type: " + getType());
    }
  }

  public String getName() {
    return name;
  }

  public DataPointer getIndexDataPointer() {
    return indexDataPointer;
  }
}
