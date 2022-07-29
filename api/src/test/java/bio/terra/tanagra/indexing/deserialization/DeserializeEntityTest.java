package bio.terra.tanagra.indexing.deserialization;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DeserializeEntityTest {
  static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() {
    Underlay underlay = Underlay.fromJSON("config/underlay/AouSynthetic.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void invalidFilePath() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Entity.fromJSON("nonexistent_entity_file.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertEquals("Error deserializing Entity from JSON", ex.getMessage());
  }

  @Test
  void noAttributes() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Entity.fromJSON("config/entity/NoAttributes.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No Attributes defined"));
  }

  @Test
  void noIdAttribute() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Entity.fromJSON("config/entity/NoIdAttribute.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No id Attribute defined"));
  }

  @Test
  void idAttributeNotFound() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Entity.fromJSON("config/entity/IdAttributeNotFound.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(
        ex.getMessage().startsWith("Id Attribute not found in the set of Attributes"));
  }

  @Test
  void noSourceDataMapping() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Entity.fromJSON("config/entity/NoSourceDataMapping.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No source Data Mapping defined"));
  }

  @Test
  void noIndexDataMapping() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Entity.fromJSON("config/entity/NoIndexDataMapping.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No index Data Mapping defined"));
  }

  @Test
  void attributeWithoutName() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Entity.fromJSON("config/entity/AttributeWithoutName.json", dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("Attribute name is undefined"));
  }
}
