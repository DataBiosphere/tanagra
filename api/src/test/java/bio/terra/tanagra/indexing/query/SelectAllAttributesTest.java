package bio.terra.tanagra.indexing.query;

import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SelectAllAttributesTest {
  static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() {
    Underlay underlay = Underlay.fromJSON("config/underlay/Omop.json");
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void person() throws IOException {
    Entity person = Entity.fromJSON("config/entity/Person.json", dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getSourceDataMapping().selectAllQuery(),
        "query/person_source_selectAllAttributes.sql");
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getIndexDataMapping().selectAllQuery(),
        "query/person_index_selectAllAttributes.sql");
  }

  @Test
  void condition() throws IOException {
    Entity person = Entity.fromJSON("config/entity/Condition.json", dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getSourceDataMapping().selectAllQuery(),
        "query/condition_source_selectAllAttributes.sql");
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getIndexDataMapping().selectAllQuery(),
        "query/condition_index_selectAllAttributes.sql");
  }
}
