package bio.terra.tanagra.indexing.deserialization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileIO;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidUnderlayTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvalidUnderlayTest.class);

  @BeforeClass
  public static void setupFileIO() {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config/"));
  }

  @Test
  public void invalidFilePath() {
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> Underlay.fromJSON("nonexistent_file_path.json"));
    LOGGER.info("expected exception", ex);
    assertEquals("Resource file not found: config/nonexistent_file_path.json", ex.getMessage());
  }

  @Test
  public void noDataPointers() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class, () -> Underlay.fromJSON("underlay/NoDataPointers.json"));
    LOGGER.info("expected exception", ex);
    assertEquals("No DataPointer defined", ex.getMessage());
  }

  @Test
  public void noEntities() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class, () -> Underlay.fromJSON("underlay/NoEntities.json"));
    LOGGER.info("expected exception", ex);
    assertEquals("No Entity defined", ex.getMessage());
  }

  @Test
  public void noPrimaryEntity() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class, () -> Underlay.fromJSON("underlay/NoPrimaryEntity.json"));
    LOGGER.info("expected exception", ex);
    assertEquals("No primary Entity defined", ex.getMessage());
  }

  @Test
  public void primaryEntityNotFound() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Underlay.fromJSON("underlay/PrimaryEntityNotFound.json"));
    LOGGER.info("expected exception", ex);
    assertEquals("Primary Entity not found in the set of Entities", ex.getMessage());
  }

  @Test
  public void noBQProjectId() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class, () -> Underlay.fromJSON("underlay/NoBQProjectId.json"));
    LOGGER.info("expected exception", ex);
    assertEquals("No BigQuery project ID defined", ex.getMessage());
  }
}
