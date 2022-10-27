package bio.terra.tanagra.indexing.query;

import bio.terra.tanagra.indexing.FileIO;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RawSqlTablePointerTest {
  private static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void allIngredients() throws IOException {
    Entity ingredient = Entity.fromJSON("RawSqlTable.json", dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        ingredient.getMapping(Underlay.MappingType.SOURCE).queryAllAttributes().renderSQL(),
            "sql/indexing/rawsql_source_allInstances.sql");
  }
}
