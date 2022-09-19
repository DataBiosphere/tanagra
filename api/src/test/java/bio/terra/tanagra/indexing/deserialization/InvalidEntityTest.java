package bio.terra.tanagra.indexing.deserialization;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidEntityTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvalidEntityTest.class);
  private static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void invalidFilePath() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Entity.fromJSON("nonexistent_entity_file.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals(
        "Resource file not found: config/entity/nonexistent_entity_file.json", ex.getMessage());
  }

  @Test
  void noAttributes() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class, () -> Entity.fromJSON("NoAttributes.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("No Attributes defined"));
  }

  @Test
  void noIdAttribute() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("NoIdAttribute.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("No id Attribute defined"));
  }

  @Test
  void idAttributeNotFound() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("IdAttributeNotFound.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(
        ex.getMessage().startsWith("Id Attribute not found in the set of Attributes"));
  }

  @Test
  void noSourceDataMapping() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("NoSourceDataMapping.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("No source Data Mapping defined"));
  }

  @Test
  void noIndexDataMapping() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("NoIndexDataMapping.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("No index Data Mapping defined"));
  }

  @Test
  void attributeWithoutName() {
    InvalidConfigException ex =
        assertThrows(
            InvalidConfigException.class,
            () -> Entity.fromJSON("AttributeWithoutName.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("Attribute name is undefined"));
  }
}
