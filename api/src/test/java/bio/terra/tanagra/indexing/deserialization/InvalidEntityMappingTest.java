package bio.terra.tanagra.indexing.deserialization;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InvalidEntityMappingTest {
  static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() {
    Underlay underlay = Underlay.fromJSON("config/underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void mappingForNonExistentAttribute() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Entity.fromJSON("config/entity/MappingNonExistentAttribute.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(
        ex.getMessage().startsWith("A mapping is defined for a non-existent attribute"));
  }

  @Test
  void doubleTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Entity.fromJSON("config/entity/DoubleTextSearchMapping.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertEquals(
        "Text search mapping can be defined by either attributes or a search string, not both",
        ex.getMessage());
  }

  @Test
  void emptyTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Entity.fromJSON("config/entity/EmptyTextSearchMapping.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertEquals("Text search mapping is empty", ex.getMessage());
  }

  @Test
  void emptyAttributesListTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                Entity.fromJSON(
                    "config/entity/EmptyAttributesListTextSearchMapping.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertEquals("Text search mapping list of attributes is empty", ex.getMessage());
  }
}
