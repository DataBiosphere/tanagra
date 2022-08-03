package bio.terra.tanagra.indexing.query;

import static bio.terra.tanagra.indexing.Indexer.READ_RESOURCE_FILE_FUNCTION;

import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TextSearchMappingTest {
  private static Map<String, DataPointer> dataPointers;

  @BeforeAll
  static void readDataPointers() throws IOException {
    Underlay underlay = Underlay.fromJSON("config/underlay/Omop.json", READ_RESOURCE_FILE_FUNCTION);
    dataPointers = underlay.getDataPointers();
  }

  @Test
  void condition() throws IOException {
    Entity condition =
        Entity.fromJSON("config/entity/Condition.json", READ_RESOURCE_FILE_FUNCTION, dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        condition.getSourceDataMapping().queryTextSearchStrings().renderSQL(),
        "query/condition_source_textSearch.sql");
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        condition.getIndexDataMapping().queryTextSearchStrings().renderSQL(),
        "query/condition_index_textSearch.sql");
  }
}
