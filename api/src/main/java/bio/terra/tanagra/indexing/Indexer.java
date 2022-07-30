package bio.terra.tanagra.indexing;

import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.util.List;

public class Indexer {
  public Indexer() {}

  public void indexUnderlay(String underlayResourceFilePath) throws IOException {
    // deserialize the POJOs to the internal objects and expand all defaults
    Underlay underlay = Underlay.fromJSON(underlayResourceFilePath);

    // build a list of indexing commands, including their associated input queries
    List<WorkflowCommand> indexingCmds = underlay.getIndexingCommands();

    // convert the internal objects, now expanded, back to POJOs

    // write out all index data commands into a script

    // write out the now expanded POJOs to a new file

  }
}
