package bio.terra.tanagra.indexing;

import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Indexer {
  private static final Logger logger = LoggerFactory.getLogger(Indexer.class);

  public static String OUTPUT_UNDERLAY_FILE_EXTENSION = ".json";

  public Indexer() {}

  public static void indexUnderlay(String underlayResourceFilePath) throws IOException {
    // deserialize the POJOs to the internal objects and expand all defaults
    Underlay underlay = Underlay.fromJSON(underlayResourceFilePath);

    // build a list of indexing commands, including their associated input queries
    List<WorkflowCommand> indexingCmds = underlay.getIndexingCommands();

    // write out all index input files and commands into a script
    File outputDir = Path.of(underlay.getName()).toFile();
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    logger.info("Writing output to directory: {}", outputDir.getAbsolutePath());
    WorkflowCommand.writeToDisk(indexingCmds, outputDir.toPath());

    // convert the internal objects, now expanded, back to POJOs
    UFUnderlay expandedUnderlay = new UFUnderlay(underlay);
    List<UFEntity> expandedEntities =
        underlay.getEntities().values().stream()
            .map(e -> new UFEntity(e))
            .collect(Collectors.toList());

    // write out the expanded POJOs
    JacksonMapper.writeJavaObjectToFile(
        outputDir.toPath().resolve(expandedUnderlay.name + OUTPUT_UNDERLAY_FILE_EXTENSION),
        expandedUnderlay);
    for (UFEntity expandedEntity : expandedEntities) {
      JacksonMapper.writeJavaObjectToFile(
          outputDir.toPath().resolve(expandedEntity.name + OUTPUT_UNDERLAY_FILE_EXTENSION),
          expandedEntity);
    }
  }
}
