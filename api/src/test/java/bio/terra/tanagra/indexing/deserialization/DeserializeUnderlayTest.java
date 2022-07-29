package bio.terra.tanagra.indexing.deserialization;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.underlay.Underlay;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DeserializeUnderlayTest {
  @Test
  void invalidFilePath() {
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> Underlay.fromJSON("nonexistent_file_path.json"));
    ex.printStackTrace();
    Assertions.assertEquals("Error deserializing Underlay from JSON", ex.getMessage());
  }

  @Test
  void noDataPointers() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Underlay.fromJSON("config/underlay/NoDataPointers.json"));
    ex.printStackTrace();
    Assertions.assertEquals("No DataPointer defined", ex.getMessage());
  }

  @Test
  void noEntities() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Underlay.fromJSON("config/underlay/NoEntities.json"));
    ex.printStackTrace();
    Assertions.assertEquals("No Entity defined", ex.getMessage());
  }

  @Test
  void noPrimaryEntity() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Underlay.fromJSON("config/underlay/NoPrimaryEntity.json"));
    ex.printStackTrace();
    Assertions.assertEquals("No primary Entity defined", ex.getMessage());
  }

  @Test
  void primaryEntityNotFound() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Underlay.fromJSON("config/underlay/PrimaryEntityNotFound.json"));
    ex.printStackTrace();
    Assertions.assertEquals("Primary Entity not found in the set of Entities", ex.getMessage());
  }

  @Test
  void noBQProjectId() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Underlay.fromJSON("config/underlay/NoBQProjectId.json"));
    ex.printStackTrace();
    Assertions.assertEquals("No BigQuery project ID defined", ex.getMessage());
  }
}
