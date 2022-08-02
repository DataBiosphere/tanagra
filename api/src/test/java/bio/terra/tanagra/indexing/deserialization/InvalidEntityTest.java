package bio.terra.tanagra.indexing.deserialization;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class InvalidEntityTest {
  static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    UFUnderlay serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileUtils.getResourceFileStream("config/underlay/Omop.json"), UFUnderlay.class);
    Underlay underlay = Underlay.deserialize(serialized, true);
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void invalidFilePath() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("nonexistent_entity_file.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertEquals("Error deserializing Entity from JSON", ex.getMessage());
  }

  @Test
  void noAttributes() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/entity/NoAttributes.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No Attributes defined"));
  }

  @Test
  void noIdAttribute() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/entity/NoIdAttribute.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No id Attribute defined"));
  }

  @Test
  void idAttributeNotFound() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/entity/IdAttributeNotFound.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertTrue(
        ex.getMessage().startsWith("Id Attribute not found in the set of Attributes"));
  }

  @Test
  void noSourceDataMapping() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/entity/NoSourceDataMapping.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No source Data Mapping defined"));
  }

  @Test
  void noIndexDataMapping() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/entity/NoIndexDataMapping.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("No index Data Mapping defined"));
  }

  @Test
  void attributeWithoutName() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/entity/AttributeWithoutName.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertTrue(ex.getMessage().startsWith("Attribute name is undefined"));
  }
}
