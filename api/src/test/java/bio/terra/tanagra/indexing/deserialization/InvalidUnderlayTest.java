package bio.terra.tanagra.indexing.deserialization;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InvalidUnderlayTest {
  @Test
  void invalidFilePath() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> {
              UFUnderlay serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("nonexistent_file_path.json"),
                      UFUnderlay.class);
              Underlay.deserialize(serialized, true);
            });
    ex.printStackTrace();
    Assertions.assertEquals("Error deserializing Underlay from JSON", ex.getMessage());
  }

  @Test
  void noDataPointers() throws IOException {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFUnderlay serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/underlay/NoDataPointers.json"),
                      UFUnderlay.class);
              Underlay.deserialize(serialized, true);
            });
    ex.printStackTrace();
    Assertions.assertEquals("No DataPointer defined", ex.getMessage());
  }

  @Test
  void noEntities() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFUnderlay serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/underlay/NoEntities.json"),
                      UFUnderlay.class);
              Underlay.deserialize(serialized, true);
            });
    ex.printStackTrace();
    Assertions.assertEquals("No Entity defined", ex.getMessage());
  }

  @Test
  void noPrimaryEntity() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFUnderlay serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/underlay/NoPrimaryEntity.json"),
                      UFUnderlay.class);
              Underlay.deserialize(serialized, true);
            });
    ex.printStackTrace();
    Assertions.assertEquals("No primary Entity defined", ex.getMessage());
  }

  @Test
  void primaryEntityNotFound() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFUnderlay serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/underlay/PrimaryEntityNotFound.json"),
                      UFUnderlay.class);
              Underlay.deserialize(serialized, true);
            });
    ex.printStackTrace();
    Assertions.assertEquals("Primary Entity not found in the set of Entities", ex.getMessage());
  }

  @Test
  void noBQProjectId() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              UFUnderlay serialized =
                  JacksonMapper.readFileIntoJavaObject(
                      FileUtils.getResourceFileStream("config/underlay/NoBQProjectId.json"),
                      UFUnderlay.class);
              Underlay.deserialize(serialized, true);
            });
    ex.printStackTrace();
    Assertions.assertEquals("No BigQuery project ID defined", ex.getMessage());
  }
}
