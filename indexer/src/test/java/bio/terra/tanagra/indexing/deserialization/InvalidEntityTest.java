package bio.terra.tanagra.indexing.deserialization;

import static org.junit.Assert.*;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidEntityTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvalidEntityTest.class);
  private static Map<String, DataPointer> dataPointers;

  @BeforeClass
  public static void readDataPointers() throws IOException {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  public void invalidFilePath() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Entity.fromJSON("nonexistent_entity_file.json", dataPointers));
    LOGGER.info("expected exception", ex);
    assertEquals(
        "Resource file not found: config/entity/nonexistent_entity_file.json", ex.getMessage());
  }

  @Test
  public void noAttributes() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class, () -> Entity.fromJSON("NoAttributes.json", dataPointers));
    LOGGER.info("expected exception", ex);
    assertTrue(ex.getMessage().startsWith("No Attributes defined"));
  }

  @Test
  public void noIdAttribute() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("NoIdAttribute.json", dataPointers));
    LOGGER.info("expected exception", ex);
    assertTrue(ex.getMessage().startsWith("No id Attribute defined"));
  }

  @Test
  public void idAttributeNotFound() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("IdAttributeNotFound.json", dataPointers));
    LOGGER.info("expected exception", ex);
    assertTrue(ex.getMessage().startsWith("Id Attribute not found in the set of Attributes"));
  }

  @Test
  public void noSourceDataMapping() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("NoSourceDataMapping.json", dataPointers));
    LOGGER.info("expected exception", ex);
    assertTrue(ex.getMessage().startsWith("No source Data Mapping defined"));
  }

  @Test
  public void noIndexDataMapping() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("NoIndexDataMapping.json", dataPointers));
    LOGGER.info("expected exception", ex);
    assertTrue(ex.getMessage().startsWith("No index Data Mapping defined"));
  }

  @Test
  public void attributeWithoutName() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("AttributeWithoutName.json", dataPointers));
    LOGGER.info("expected exception", ex);
    assertTrue(ex.getMessage().startsWith("Attribute name is undefined"));
  }
}
