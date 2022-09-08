package bio.terra.tanagra.indexing;

import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.serialization.UFEntityGroup;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Indexer {
  private static final Logger LOGGER = LoggerFactory.getLogger(Indexer.class);
  public static final String OUTPUT_UNDERLAY_FILE_EXTENSION = ".json";
  public static final Function<Path, InputStream> READ_RESOURCE_FILE_FUNCTION =
      filePath -> FileUtils.getResourceFileStream(filePath);
  public static final Function<Path, InputStream> READ_FILE_FUNCTION =
      filePath -> FileUtils.getFileStream(filePath);

  private final String underlayPath;
  private final Function<Path, InputStream> getFileInputStreamFunction;

  private List<WorkflowCommand> indexingCmds;
  private UFUnderlay expandedUnderlay;
  private List<UFEntity> expandedEntities;
  private List<UFEntityGroup> expandedEntityGroups;

  private Indexer(String underlayPath, Function<Path, InputStream> getFileInputStreamFunction) {
    this.underlayPath = underlayPath;
    this.getFileInputStreamFunction = getFileInputStreamFunction;
  }

  public static Indexer fromResourceFile(String underlayResourceFilePath) {
    return new Indexer(underlayResourceFilePath, READ_RESOURCE_FILE_FUNCTION);
  }

  public static Indexer fromFile(String underlayFilePath) {
    return new Indexer(underlayFilePath, READ_FILE_FUNCTION);
  }

  public void indexUnderlay() throws IOException {
    // deserialize the POJOs to the internal objects and expand all defaults
    Underlay underlay = Underlay.fromJSON(Path.of(underlayPath), getFileInputStreamFunction);

    // scan the source data to lookup data types, generate UI hints, etc.
    underlay.getEntities().values().forEach(Entity::scanSourceData);

    // build a list of indexing commands, including their associated input queries
    indexingCmds = underlay.getIndexingCommands();

    // convert the internal objects, now expanded, back to POJOs
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

  public void writeOutIndexFiles(String outputDir) throws IOException {
    // write out all index input files and commands into a script
    Path outputDirPath = Path.of(outputDir);
    if (!outputDirPath.toFile().exists()) {
      boolean mkdirsSuccess = outputDirPath.toFile().mkdirs();
      if (!mkdirsSuccess) {
        throw new IOException("mkdirs failed for output dir: " + outputDirPath);
      }
    }
    LOGGER.info("Writing output to directory: {}", outputDirPath.toAbsolutePath());
    WorkflowCommand.writeToDisk(indexingCmds, outputDirPath);

    // write out the expanded POJOs
    JacksonMapper.writeJavaObjectToFile(
        outputDirPath.resolve(expandedUnderlay.getName() + OUTPUT_UNDERLAY_FILE_EXTENSION),
        expandedUnderlay);
    for (UFEntity expandedEntity : expandedEntities) {
      JacksonMapper.writeJavaObjectToFile(
          outputDirPath.resolve(expandedEntity.getName() + OUTPUT_UNDERLAY_FILE_EXTENSION),
          expandedEntity);
    }
    for (UFEntityGroup expandedEntityGroup : expandedEntityGroups) {
      JacksonMapper.writeJavaObjectToFile(
          outputDirPath.resolve(expandedEntityGroup.getName() + OUTPUT_UNDERLAY_FILE_EXTENSION),
          expandedEntityGroup);
    }
  }

  public static void main(String... args) throws Exception {
    String underlayFilePath = args[0];
    String outputDirPath = args[1];

    Indexer indexer = Indexer.fromFile(underlayFilePath);
    indexer.indexUnderlay();
    indexer.writeOutIndexFiles(outputDirPath);
  }
}
