package bio.terra.tanagra.indexing.command;

import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.indexing.Indexer;
import bio.terra.tanagra.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class IndexUnderlayTest {
  @Test
  void omop() throws IOException {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    FileIO.setOutputParentDir(Path.of("test_output"));
    FileUtils.createDirectoryIfNonexistent(FileIO.getOutputParentDir());

    Indexer indexer = Indexer.deserializeUnderlay("underlay/Omop.json");
    indexer.scanSourceData();

    indexer.serializeUnderlay();
    indexer.writeSerializedUnderlay();
  }
}
