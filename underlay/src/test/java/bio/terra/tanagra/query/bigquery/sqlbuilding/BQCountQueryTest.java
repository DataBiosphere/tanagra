package bio.terra.tanagra.query.bigquery.sqlbuilding;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.query.hint.HintInstance;
import bio.terra.tanagra.api.query.hint.HintQueryResult;
import bio.terra.tanagra.api.shared.*;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class BQCountQueryTest extends BQRunnerTest {
  @Test
  void withFilter() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute("gender"),
            BinaryOperator.NOT_EQUALS,
            Literal.forInt64(8_207L));
    AttributeField groupByAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay,
                entity,
                null,
                List.of(groupByAttribute),
                attributeFilter,
                OrderByDirection.DESCENDING,
                null,
                null,
                null,
                null,
                true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("withFilter", countQueryResult.getSql(), entityMainTable);
  }

  @Test
  void noFilter() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField groupByAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay,
                entity,
                null,
                List.of(groupByAttribute),
                null,
                OrderByDirection.DESCENDING,
                null,
                null,
                null,
                null,
                true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("noFilter", countQueryResult.getSql(), entityMainTable);
  }

  @Test
  void noGroupByFields() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay,
                entity,
                null,
                List.of(),
                null,
                OrderByDirection.DESCENDING,
                null,
                null,
                null,
                null,
                true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "noGroupByFields", countQueryResult.getSql(), entityMainTable);
  }

  @Test
  void groupByRuntimeCalculatedField() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField groupByAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false);
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay,
                entity,
                null,
                List.of(groupByAttribute),
                null,
                OrderByDirection.DESCENDING,
                null,
                null,
                null,
                null,
                true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "groupByRuntimeCalculatedField", countQueryResult.getSql(), entityMainTable);
  }

  @Test
  void groupByValueDisplayAttribute() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    Attribute groupByAttribute = entity.getAttribute("gender");
    AttributeField groupByAttributeField =
        new AttributeField(underlay, entity, groupByAttribute, false);
    HintQueryResult hintQueryResult =
        new HintQueryResult(
            "",
            List.of(
                new HintInstance(
                    groupByAttribute,
                    Map.of(new ValueDisplay(Literal.forInt64(8_532L), "Female"), 100L))));
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay,
                entity,
                null,
                List.of(groupByAttributeField),
                null,
                OrderByDirection.DESCENDING,
                null,
                null,
                null,
                hintQueryResult,
                true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "groupByValueDisplayField", countQueryResult.getSql(), entityMainTable);
  }

  @Test
  void groupByRepeatedAttribute() throws IOException {
    Entity entity = underlay.getEntity("condition");

    // We don't have an example of an attribute with a repeated data type, yet.
    // So create an artificial attribute just for this test.
    Attribute groupByAttribute =
        new Attribute(
            "vocabulary",
            DataType.STRING,
            true,
            false,
            false,
            "['foo', 'bar', 'baz', ${fieldSql}]",
            DataType.STRING,
            entity.getAttribute("vocabulary").isComputeDisplayHint(),
            entity.getAttribute("vocabulary").isSuppressedForExport(),
            entity.getAttribute("vocabulary").isVisitDateForTemporalQuery(),
            entity.getAttribute("vocabulary").isVisitIdForTemporalQuery(),
            entity.getAttribute("vocabulary").getSourceQuery());
    AttributeField groupByAttributeField =
        new AttributeField(underlay, entity, groupByAttribute, false);
    HintQueryResult hintQueryResult =
        new HintQueryResult(
            "",
            List.of(
                new HintInstance(
                    groupByAttribute,
                    Map.of(
                        new ValueDisplay(Literal.forString("foo")),
                        25L,
                        new ValueDisplay(Literal.forString("bar")),
                        140L,
                        new ValueDisplay(Literal.forString("baz")),
                        85L))));
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay,
                entity,
                null,
                List.of(groupByAttributeField),
                null,
                OrderByDirection.DESCENDING,
                null,
                null,
                null,
                hintQueryResult,
                true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "groupByRepeatedAttribute", countQueryResult.getSql(), entityMainTable);
  }

  @Test
  void countDistinctAttributeNotId() throws IOException {
    Entity entity = underlay.getEntity("conditionOccurrence");
    Attribute countDistinctAttribute = entity.getAttribute("person_id");
    Attribute groupByAttribute = entity.getAttribute("condition");
    AttributeField groupByAttributeField =
        new AttributeField(underlay, entity, groupByAttribute, false);
    HintQueryResult hintQueryResult =
        new HintQueryResult(
            "",
            List.of(
                new HintInstance(
                    groupByAttribute,
                    Map.of(new ValueDisplay(Literal.forInt64(8_532L), "Female"), 100L))));
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay,
                entity,
                countDistinctAttribute,
                List.of(groupByAttributeField),
                null,
                OrderByDirection.DESCENDING,
                null,
                null,
                null,
                hintQueryResult,
                true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "countDistinctAttributeNotId", countQueryResult.getSql(), entityMainTable);
  }
}
