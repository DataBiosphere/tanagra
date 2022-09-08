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

public class SelectAllAttributesTest {
  private static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    FileIO.setToReadResourceFiles();
    Underlay underlay = Underlay.fromJSON(Path.of("config/underlay/Omop.json"));
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void person() throws IOException {
    Entity person = Entity.fromJSON(Path.of("config/entity/Person.json"), dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getSourceDataMapping().queryAttributes(person.getAttributes()).renderSQL(),
        "query/person_source_selectAllAttributes.sql");
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getIndexDataMapping().queryAttributes(person.getAttributes()).renderSQL(),
        "query/person_index_selectAllAttributes.sql");
  }

  @Test
  void condition() throws IOException {
    Entity condition = Entity.fromJSON(Path.of("config/entity/Condition.json"), dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        condition.getSourceDataMapping().queryAttributes(condition.getAttributes()).renderSQL(),
        "query/condition_source_selectAllAttributes.sql");
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        condition.getIndexDataMapping().queryAttributes(condition.getAttributes()).renderSQL(),
        "query/condition_index_selectAllAttributes.sql");
  }
}
