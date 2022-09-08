package bio.terra.tanagra.indexing.deserialization;

import static bio.terra.tanagra.indexing.Indexer.READ_RESOURCE_FILE_FUNCTION;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    Underlay underlay =
        Underlay.fromJSON(Path.of("config/underlay/Omop.json"), READ_RESOURCE_FILE_FUNCTION);
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void invalidFilePath() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                Entity.fromJSON(
                    Path.of("nonexistent_entity_file.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals(
        "Resource file not found: nonexistent_entity_file.json", ex.getMessage());
  }

  @Test
  void noAttributes() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/NoAttributes.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("No Attributes defined"));
  }

  @Test
  void noIdAttribute() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/NoIdAttribute.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("No id Attribute defined"));
  }

  @Test
  void idAttributeNotFound() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/IdAttributeNotFound.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(
        ex.getMessage().startsWith("Id Attribute not found in the set of Attributes"));
  }

  @Test
  void noSourceDataMapping() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/NoSourceDataMapping.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("No source Data Mapping defined"));
  }

  @Test
  void noIndexDataMapping() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/NoIndexDataMapping.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("No index Data Mapping defined"));
  }

  @Test
  void attributeWithoutName() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/AttributeWithoutName.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(ex.getMessage().startsWith("Attribute name is undefined"));
  }
}
