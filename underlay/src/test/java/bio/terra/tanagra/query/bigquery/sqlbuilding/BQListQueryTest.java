package bio.terra.tanagra.query.bigquery.sqlbuilding;

import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQListQueryTest extends BQRunnerTest {
  @Test
  void noSelectFields() {
    Entity entity = underlay.getPrimaryEntity();
    assertThrows(
        InvalidQueryException.class,
        () ->
            bqQueryRunner.run(
                ListQueryRequest.dryRunAgainstIndexData(
                    underlay, entity, List.of(), null, null, null)));
  }

  @Test
  void noFilter() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), null, null, null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("noFilter", listQueryResult.getSql(), table);
  }

  @Test
  void withOrderByInSelect() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField selectAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                entity,
                List.of(selectAttribute),
                null,
                List.of(new ListQueryRequest.OrderBy(selectAttribute, OrderByDirection.DESCENDING)),
                null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("withOrderByInSelect", listQueryResult.getSql(), table);
  }

  @Test
  void withOrderByNotInSelect() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField selectAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    AttributeField orderByAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("age"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                entity,
                List.of(selectAttribute),
                null,
                List.of(new ListQueryRequest.OrderBy(orderByAttribute, OrderByDirection.ASCENDING)),
                null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("withOrderByNotInSelect", listQueryResult.getSql(), table);
  }

  @Test
  void withOrderByValueDisplayAttribute() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField selectAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    AttributeField orderByAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("gender"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                entity,
                List.of(selectAttribute),
                null,
                List.of(new ListQueryRequest.OrderBy(orderByAttribute, OrderByDirection.ASCENDING)),
                null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "withOrderByValueDisplayAttribute", listQueryResult.getSql(), table);
  }

  @Test
  void withOrderByRandom() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField selectAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay,
                entity,
                List.of(selectAttribute),
                null,
                List.of(ListQueryRequest.OrderBy.random()),
                45));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("withOrderByRandom", listQueryResult.getSql(), table);
  }

  @Test
  void withLimit() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField selectAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(selectAttribute), null, List.of(), 45));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("withLimit", listQueryResult.getSql(), table);
  }
}
