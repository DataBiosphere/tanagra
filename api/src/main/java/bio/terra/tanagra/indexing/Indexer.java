package bio.terra.tanagra.indexing;

import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;

public class Indexer {
  public Indexer() {}

  public void indexUnderlay(String underlayResourceFilePath) throws IOException {
    // convert the POJOs to the internal objects: Underlay
    Underlay underlay = Underlay.fromJSON(underlayResourceFilePath);

    // for each entity:
    // expand the source data mapping

    // generate the index data mapping

    // generate the index data commands

    // for each entity group:
    // generate any index data

    // generate the index data commands

    // write out all index data commands into a script

    // convert the internal objects, now expanded, back to POJOs

    // write out the now expanded POJOs to a new file

  }
}
