package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.Indexer;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class IndexUnderlayTest {
  @Test
  void omop() throws IOException {
    Indexer.indexUnderlay("config/underlay/Omop.json");
  }
}
