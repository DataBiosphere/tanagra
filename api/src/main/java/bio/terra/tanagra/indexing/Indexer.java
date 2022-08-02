package bio.terra.tanagra.indexing;

import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Indexer {
  private static final Logger logger = LoggerFactory.getLogger(Indexer.class);
  public static String OUTPUT_UNDERLAY_FILE_EXTENSION = ".json";

  private boolean isFromResourceFile;
  private InputStream underlayInputStream;

  private Underlay underlay;
  private List<WorkflowCommand> indexingCmds;
  private UFUnderlay expandedUnderlay;
  private List<UFEntity> expandedEntities;

  private Indexer(InputStream underlayInputStream, boolean isFromResourceFile) {
    this.isFromResourceFile = isFromResourceFile;
    this.underlayInputStream = underlayInputStream;
  }

  public static Indexer fromResourceFile(String underlayResourceFilePath) throws IOException {
    return new Indexer(FileUtils.getResourceFileStream(underlayResourceFilePath), true);
  }

  public static Indexer fromFile(Path underlayFilePath) throws FileNotFoundException {
    return new Indexer(new FileInputStream(underlayFilePath.toFile()), false);
  }

  public void indexUnderlay() throws IOException {
    // deserialize the POJOs to the internal objects and expand all defaults
    UFUnderlay serialized =
        JacksonMapper.readFileIntoJavaObject(underlayInputStream, UFUnderlay.class);
    underlay = Underlay.deserialize(serialized, isFromResourceFile);

    // build a list of indexing commands, including their associated input queries
    indexingCmds = underlay.getIndexingCommands();

    // convert the internal objects, now expanded, back to POJOs
    expandedUnderlay = new UFUnderlay(underlay);
    expandedEntities =
        underlay.getEntities().values().stream()
            .map(e -> new UFEntity(e))
            .collect(Collectors.toList());
  }

  public void writeOutIndexFiles(Path outputDir) throws IOException {
    // write out all index input files and commands into a script
    if (!outputDir.toFile().exists()) {
      outputDir.toFile().mkdirs();
    }
    logger.info("Writing output to directory: {}", outputDir.toAbsolutePath());
    WorkflowCommand.writeToDisk(indexingCmds, outputDir);

    // write out the expanded POJOs
    JacksonMapper.writeJavaObjectToFile(
        outputDir.resolve(expandedUnderlay.name + OUTPUT_UNDERLAY_FILE_EXTENSION),
        expandedUnderlay);
    for (UFEntity expandedEntity : expandedEntities) {
      JacksonMapper.writeJavaObjectToFile(
          outputDir.resolve(expandedEntity.name + OUTPUT_UNDERLAY_FILE_EXTENSION), expandedEntity);
    }
  }

  public static void main(String... args) throws Exception {
    Path underlayFilePath = Path.of(args[0]);
    Path outputDirPath = Path.of(args[1]);

    Indexer indexer = Indexer.fromFile(underlayFilePath);
    indexer.indexUnderlay();
    indexer.writeOutIndexFiles(outputDirPath);
  }
}
