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

public class SelectAllAttributesTest {
  private static Map<String, DataPointer> dataPointers;

  @BeforeClass
  public static void readDataPointers() throws IOException {
    FileIO.setToReadResourceFiles();
    FileIO.setInputParentDir(Path.of("config"));
    Underlay underlay = Underlay.fromJSON("underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  public void person() throws IOException {
    Entity person = Entity.fromJSON("Person.json", dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getMapping(Underlay.MappingType.SOURCE).queryAllAttributes().renderSQL(),
        "generatedSql/person_source_selectAllAttributes.sql");
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getMapping(Underlay.MappingType.INDEX).queryAllAttributes().renderSQL(),
        "generatedSql/person_index_selectAllAttributes.sql");
  }

  @Test
  public void condition() throws IOException {
    Entity condition = Entity.fromJSON("Condition.json", dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        condition.getMapping(Underlay.MappingType.SOURCE).queryAllAttributes().renderSQL(),
        "generatedSql/condition_source_selectAllAttributes.sql");
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        condition.getMapping(Underlay.MappingType.INDEX).queryAllAttributes().renderSQL(),
        "generatedSql/condition_index_selectAllAttributes.sql");
  }
}
