package bio.terra.tanagra.indexing.deserialization;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

public class InvalidEntityMappingTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(InvalidEntityMappingTest.class);
  private static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void mappingForNonExistentAttribute() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Entity.fromJSON("MappingNonExistentAttribute.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertTrue(
        ex.getMessage().startsWith("A source mapping is defined for a non-existent attribute"));
  }

  @Test
  void doubleTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Entity.fromJSON("DoubleTextSearchMapping.json", dataPointers));
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
            () -> Entity.fromJSON("EmptyTextSearchMapping.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("Text search mapping is empty", ex.getMessage());
  }

  @Test
  void emptyAttributesListTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Entity.fromJSON("EmptyAttributesListTextSearchMapping.json", dataPointers));
    LOGGER.info("expected exception", ex);
    Assertions.assertEquals("Text search mapping list of attributes is empty", ex.getMessage());
  }
}
