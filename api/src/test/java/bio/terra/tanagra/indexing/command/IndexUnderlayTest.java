package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.Indexer;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class IndexUnderlayTest {
  @Test
  void omop() throws IOException {
    FileIO.setToReadResourceFiles();
    Indexer omopUnderlay = new Indexer("config/underlay/Omop.json");
    omopUnderlay.indexUnderlay();
    omopUnderlay.writeOutIndexFiles("omop");
  }
}
