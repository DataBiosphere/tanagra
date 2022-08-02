package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.Indexer;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class IndexUnderlayTest {
  @Test
  void omop() throws IOException {
    Indexer omopUnderlay = Indexer.fromResourceFile("config/underlay/Omop.json");
    omopUnderlay.indexUnderlay();
    omopUnderlay.writeOutIndexFiles(Path.of("omop"));
  }
}
