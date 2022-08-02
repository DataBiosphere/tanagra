package bio.terra.tanagra.indexing.query;

import bio.terra.tanagra.serialization.UFEntity;
import bio.terra.tanagra.serialization.UFUnderlay;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.utils.FileUtils;
import bio.terra.tanagra.utils.JacksonMapper;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SelectAllAttributesTest {
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
  void person() throws IOException {
    UFEntity serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileUtils.getResourceFileStream("config/entity/Person.json"), UFEntity.class);
    Entity person = Entity.deserialize(serialized, dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getSourceDataMapping().selectAllQuery(),
        "query/person_source_selectAllAttributes.sql");
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getIndexDataMapping().selectAllQuery(),
        "query/person_index_selectAllAttributes.sql");
  }

  @Test
  void condition() throws IOException {
    UFEntity serialized =
        JacksonMapper.readFileIntoJavaObject(
            FileUtils.getResourceFileStream("config/entity/Condition.json"), UFEntity.class);
    Entity person = Entity.deserialize(serialized, dataPointers);
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getSourceDataMapping().selectAllQuery(),
        "query/condition_source_selectAllAttributes.sql");
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        person.getIndexDataMapping().selectAllQuery(),
        "query/condition_index_selectAllAttributes.sql");
  }
}
