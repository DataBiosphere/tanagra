package bio.terra.tanagra.indexing.deserialization;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidUnderlayTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvalidUnderlayTest.class);

  @BeforeAll
  static void setupFileIO() {
    FileIO.setToReadResourceFiles();
  }

  @Test
  void invalidFilePath() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> Underlay.fromJSON(Path.of("nonexistent_file_path.json")));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("Resource file not found: nonexistent_file_path.json", ex.getMessage());
  }

  @Test
  void noDataPointers() throws IOException {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Underlay.fromJSON(Path.of("config/underlay/NoDataPointers.json")));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("No DataPointer defined", ex.getMessage());
  }

  @Test
  void noEntities() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Underlay.fromJSON(Path.of("config/underlay/NoEntities.json")));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("No Entity defined", ex.getMessage());
  }

  @Test
  void noPrimaryEntity() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Underlay.fromJSON(Path.of("config/underlay/NoPrimaryEntity.json")));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("No primary Entity defined", ex.getMessage());
  }

  @Test
  void primaryEntityNotFound() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Underlay.fromJSON(Path.of("config/underlay/PrimaryEntityNotFound.json")));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("Primary Entity not found in the set of Entities", ex.getMessage());
  }

  @Test
  void noBQProjectId() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Underlay.fromJSON(Path.of("config/underlay/NoBQProjectId.json")));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("No BigQuery project ID defined", ex.getMessage());
  }
}
