package bio.terra.tanagra.indexing.query;

import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

public class RawSqlTablePointerTest {
  private static Map<String, DataPointer> dataPointers;

  @BeforeClass
  public static void readDataPointers() throws IOException {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  public void allIngredients() throws IOException {
    Entity ingredient = Entity.fromJSON("RawSqlTable.json", dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        ingredient.getMapping(Underlay.MappingType.SOURCE).queryAllAttributes().renderSQL(),
        "generatedSql/rawsql_source_allInstances.sql");
  }
}
