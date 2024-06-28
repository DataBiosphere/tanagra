package bio.terra.tanagra.query.bigquery.sqlbuilding.filter;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQTextSearchFilterTest extends BQRunnerTest {
  @Test
  void textSearchFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");
    TextSearchFilter textSearchFilter =
        new TextSearchFilter(
            underlay, entity, TextSearchFilter.TextSearchOperator.EXACT_MATCH, "diabetes", null);
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), textSearchFilter, null, null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("textSearchFilterIndex", listQueryResult.getSql(), table);

    textSearchFilter =
        new TextSearchFilter(
            underlay,
            entity,
            TextSearchFilter.TextSearchOperator.EXACT_MATCH,
            "diabetes",
            entity.getAttribute("name"));
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), textSearchFilter, null, null));
    assertSqlMatchesWithTableNameOnly("textSearchFilterAttribute", listQueryResult.getSql(), table);
  }
}
