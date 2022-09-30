package bio.terra.tanagra.indexing;

import static bio.terra.tanagra.underlay.Entity.ENTITY_DIRECTORY_NAME;
import static bio.terra.tanagra.underlay.EntityGroup.ENTITY_GROUP_DIRECTORY_NAME;

import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Indexer {
  private static final Logger LOGGER = LoggerFactory.getLogger(Indexer.class);
  public static final String OUTPUT_UNDERLAY_FILE_EXTENSION = ".json";

  private final Underlay underlay;
  private UFUnderlay expandedUnderlay;
  private List<UFEntity> expandedEntities;
  private List<UFEntityGroup> expandedEntityGroups;

  private Indexer(Underlay underlay) {
    this.underlay = underlay;
  }

  /** Deserialize the POJOs to the internal objects and expand all defaults. */
  public static Indexer deserializeUnderlay(String underlayFileName) throws IOException {
    return new Indexer(Underlay.fromJSON(underlayFileName));
  }

  /** Scan the source data to validate data pointers, lookup data types, generate UI hints, etc. */
  public void scanSourceData() {
    // TODO: Validate existence and access for data/table/field pointers.
    underlay
        .getEntities()
        .values()
        .forEach(
            e -> {
              LOGGER.info(
                  "Looking up attribute data types and generating UI hints for entity: "
                      + e.getName());
              e.scanSourceData();
            });
  }

  /** Convert the internal objects, now expanded, back to POJOs. */
  public void serializeUnderlay() {
    LOGGER.info("Serializing expanded underlay objects");
    expandedUnderlay = new UFUnderlay(underlay);
    expandedEntities =
        underlay.getEntities().values().stream()
            .map(e -> new UFEntity(e))
            .collect(Collectors.toList());
    expandedEntityGroups =
        underlay.getEntityGroups().values().stream()
            .map(eg -> new UFEntityGroup(eg))
            .collect(Collectors.toList());
  }

  /** Write out the expanded POJOs. */
  public void writeSerializedUnderlay() throws IOException {
    // Write out the underlay POJO to the top-level directory.
    Path underlayPath =
        FileIO.getOutputParentDir()
            .resolve(expandedUnderlay.getName() + OUTPUT_UNDERLAY_FILE_EXTENSION);
    JacksonMapper.writeJavaObjectToFile(underlayPath, expandedUnderlay);

    // Write out the entity POJOs to the entity/ sub-directory.
    Path entitySubDir = FileIO.getOutputParentDir().resolve(ENTITY_DIRECTORY_NAME);
    for (UFEntity expandedEntity : expandedEntities) {
      JacksonMapper.writeJavaObjectToFile(
          entitySubDir.resolve(expandedEntity.getName() + OUTPUT_UNDERLAY_FILE_EXTENSION),
          expandedEntity);
    }

    // Write out the entity group POJOs to the entity_group/ sub-directory.
    Path entityGroupSubDir = FileIO.getOutputParentDir().resolve(ENTITY_GROUP_DIRECTORY_NAME);
    for (UFEntityGroup expandedEntityGroup : expandedEntityGroups) {
      JacksonMapper.writeJavaObjectToFile(
          entityGroupSubDir.resolve(expandedEntityGroup.getName() + OUTPUT_UNDERLAY_FILE_EXTENSION),
          expandedEntityGroup);
    }
  }

  public Underlay getUnderlay() {
    return underlay;
  }
}
