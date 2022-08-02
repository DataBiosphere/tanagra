package bio.terra.tanagra.indexing;

import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Indexer {
  public Indexer() {}

  public static void indexUnderlay(String underlayResourceFilePath) throws IOException {
    // deserialize the POJOs to the internal objects and expand all defaults
    Underlay underlay = Underlay.fromJSON(underlayResourceFilePath);

    // build a list of indexing commands, including their associated input queries
    List<WorkflowCommand> indexingCmds = underlay.getIndexingCommands();

    // write out all index input files and commands into a script
    Path outputDir = Path.of(underlay.getName());
    if (!outputDir.toFile().exists()) {
      outputDir.toFile().mkdirs();
    }
    List<String> script = new ArrayList<>();
    for (WorkflowCommand cmd : indexingCmds) {
      cmd.writeInputsToDisk(outputDir);
      script.addAll(List.of(cmd.getComment(), cmd.getCommand(), ""));
    }
    Files.write(outputDir.resolve("indexing_script.sh"), script);

    // convert the internal objects, now expanded, back to POJOs

    // write out the now expanded POJOs to a new file

  }
}
