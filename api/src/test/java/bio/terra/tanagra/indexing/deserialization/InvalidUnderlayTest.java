package bio.terra.tanagra.indexing.deserialization;

import static bio.terra.tanagra.indexing.Indexer.READ_RESOURCE_FILE_FUNCTION;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InvalidUnderlayTest {
  @Test
  void invalidFilePath() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> Underlay.fromJSON("nonexistent_file_path.json", READ_RESOURCE_FILE_FUNCTION));
    ex.printStackTrace();
    Assertions.assertEquals("Resource file not found: nonexistent_file_path.json", ex.getMessage());
  }

  @Test
  void noDataPointers() throws IOException {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Underlay.fromJSON(
                    "config/underlay/NoDataPointers.json", READ_RESOURCE_FILE_FUNCTION));
    ex.printStackTrace();
    Assertions.assertEquals("No DataPointer defined", ex.getMessage());
  }

  @Test
  void noEntities() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Underlay.fromJSON("config/underlay/NoEntities.json", READ_RESOURCE_FILE_FUNCTION));
    ex.printStackTrace();
    Assertions.assertEquals("No Entity defined", ex.getMessage());
  }

  @Test
  void noPrimaryEntity() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Underlay.fromJSON(
                    "config/underlay/NoPrimaryEntity.json", READ_RESOURCE_FILE_FUNCTION));
    ex.printStackTrace();
    Assertions.assertEquals("No primary Entity defined", ex.getMessage());
  }

  @Test
  void primaryEntityNotFound() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Underlay.fromJSON(
                    "config/underlay/PrimaryEntityNotFound.json", READ_RESOURCE_FILE_FUNCTION));
    ex.printStackTrace();
    Assertions.assertEquals("Primary Entity not found in the set of Entities", ex.getMessage());
  }

  @Test
  void noBQProjectId() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Underlay.fromJSON(
                    "config/underlay/NoBQProjectId.json", READ_RESOURCE_FILE_FUNCTION));
    ex.printStackTrace();
    Assertions.assertEquals("No BigQuery project ID defined", ex.getMessage());
  }
}
