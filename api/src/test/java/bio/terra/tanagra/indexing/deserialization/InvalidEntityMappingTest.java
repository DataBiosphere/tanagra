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

public class InvalidEntityMappingTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvalidEntityMappingTest.class);
  private static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    Underlay underlay =
        Underlay.fromJSON(Path.of("config/underlay/Omop.json"), READ_RESOURCE_FILE_FUNCTION);
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void mappingForNonExistentAttribute() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/MappingNonExistentAttribute.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(
        ex.getMessage().startsWith("A mapping is defined for a non-existent attribute"));
  }

  @Test
  void doubleTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/DoubleTextSearchMapping.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals(
        "Text search mapping can be defined by either attributes or a search string, not both",
        ex.getMessage());
  }

  @Test
  void emptyTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/EmptyTextSearchMapping.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("Text search mapping is empty", ex.getMessage());
  }

  @Test
  void emptyAttributesListTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                Entity.fromJSON(
                    Path.of("config/entity/EmptyAttributesListTextSearchMapping.json"),
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("Text search mapping list of attributes is empty", ex.getMessage());
  }
}
