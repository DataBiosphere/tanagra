package bio.terra.tanagra.indexing.deserialization;

import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileIO;
import bio.terra.tanagra.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class SerDesUnderlayTest {
  @Test
  void omop() throws IOException {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    FileIO.setOutputParentDir(Path.of("test_output"));
    FileUtils.createDirectoryIfNonexistent(FileIO.getOutputParentDir());

    // Deserialize and re-serialize to test the conversion both ways.
    Underlay deserialized = Underlay.fromJSON("underlay/Omop.json");
    deserialized.serializeAndWriteToFile();
  }
}
