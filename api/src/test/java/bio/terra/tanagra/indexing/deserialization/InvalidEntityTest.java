package bio.terra.tanagra.indexing.deserialization;

import static bio.terra.tanagra.indexing.Indexer.READ_RESOURCE_FILE_FUNCTION;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InvalidEntityTest {
  private static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    Underlay underlay = Underlay.fromJSON("config/underlay/Omop.json", READ_RESOURCE_FILE_FUNCTION);
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void invalidFilePath() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                Entity.fromJSON(
                    "nonexistent_entity_file.json", READ_RESOURCE_FILE_FUNCTION, dataPointers));
    ex.printStackTrace();
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
                    "config/entity/NoAttributes.json", READ_RESOURCE_FILE_FUNCTION, dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No Attributes defined"));
  }

  @Test
  void noIdAttribute() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    "config/entity/NoIdAttribute.json", READ_RESOURCE_FILE_FUNCTION, dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No id Attribute defined"));
  }

  @Test
  void idAttributeNotFound() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    "config/entity/IdAttributeNotFound.json",
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    ex.printStackTrace();
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
                    "config/entity/NoSourceDataMapping.json",
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No source Data Mapping defined"));
  }

  @Test
  void noIndexDataMapping() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    "config/entity/NoIndexDataMapping.json",
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No index Data Mapping defined"));
  }

  @Test
  void attributeWithoutName() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Entity.fromJSON(
                    "config/entity/AttributeWithoutName.json",
                    READ_RESOURCE_FILE_FUNCTION,
                    dataPointers));
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("Attribute name is undefined"));
  }
}
