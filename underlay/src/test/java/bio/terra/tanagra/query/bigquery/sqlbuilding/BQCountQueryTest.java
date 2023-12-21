package bio.terra.tanagra.query.bigquery.sqlbuilding;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.query.count.CountQueryRequest;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.io.IOException;
import java.util.List;
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
            new Literal(8207));
    AttributeField groupByAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false, false);
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay,
                entity,
                List.of(groupByAttribute),
                attributeFilter,
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
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false, false);
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay, entity, List.of(groupByAttribute), null, null, null, null, true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("noFilter", countQueryResult.getSql(), entityMainTable);
  }

  @Test
  void noGroupByFields() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(underlay, entity, List.of(), null, null, null, null, true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "noGroupByFields", countQueryResult.getSql(), entityMainTable);
  }

  @Test
  void groupByRuntimeCalculatedField() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField groupByAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false, false);
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay, entity, List.of(groupByAttribute), null, null, null, null, true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "groupByRuntimeCalculatedField", countQueryResult.getSql(), entityMainTable);
  }

  @Test
  void groupByValueDisplayAttribute() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField groupByAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("gender"), false, false);
    CountQueryResult countQueryResult =
        bqQueryRunner.run(
            new CountQueryRequest(
                underlay, entity, List.of(groupByAttribute), null, null, null, null, true));
    BQTable entityMainTable =
        underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "groupByValueDisplayField", countQueryResult.getSql(), entityMainTable);
  }
}
