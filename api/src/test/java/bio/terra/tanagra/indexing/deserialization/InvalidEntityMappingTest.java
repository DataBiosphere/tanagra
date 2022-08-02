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

public class InvalidEntityMappingTest {
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
  void mappingForNonExistentAttribute() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream(
                          "config/entity/MappingNonExistentAttribute.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertTrue(
        ex.getMessage().startsWith("A mapping is defined for a non-existent attribute"));
  }

  @Test
  void doubleTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/entity/DoubleTextSearchMapping.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
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
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/entity/EmptyTextSearchMapping.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertEquals("Text search mapping is empty", ex.getMessage());
  }

  @Test
  void emptyAttributesListTextSearchMapping() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> {
              UFEntity serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream(
                          "config/entity/EmptyAttributesListTextSearchMapping.json"),
                      UFEntity.class);
              Entity.deserialize(serialized, dataPointers);
            });
    ex.printStackTrace();
    Assertions.assertEquals("Text search mapping list of attributes is empty", ex.getMessage());
  }
}
