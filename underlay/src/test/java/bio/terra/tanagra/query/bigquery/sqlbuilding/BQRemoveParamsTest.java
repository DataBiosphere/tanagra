package bio.terra.tanagra.query.bigquery.sqlbuilding;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.bigquery.BQQueryRunner;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQRemoveParamsTest extends BQRunnerTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }

  @Test
  void int64RemoveParam() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getIdAttribute(), false);
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            entity.getIdAttribute(),
            BinaryOperator.EQUALS,
            Literal.forInt64(25L));
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));

    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("int64", listQueryResult.getSqlNoParams(), table);
  }

  @Test
  void float64RemoveParam() throws IOException {
    Entity entity = underlay.getEntity("measurementOccurrence");
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getIdAttribute(), false);
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute("value_numeric"),
            BinaryOperator.EQUALS,
            Literal.forDouble(26.54));
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));

    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("float64", listQueryResult.getSqlNoParams(), table);
  }

  @Test
  void stringRemoveParam() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getIdAttribute(), false);
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute("person_source_value"),
            BinaryOperator.EQUALS,
            Literal.forString("id12345"));
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));

    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("string", listQueryResult.getSqlNoParams(), table);
  }

  @Test
  void dateRemoveParam() throws IOException {
    // The aouSR2019q4r4 underlay does not have an example of a field with DATE type.
    // So for this test, we use the cmssynpuf underlay.
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService("cmssynpuf_broad");
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    Underlay underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
    BQQueryRunner bqQueryRunner = (BQQueryRunner) underlay.getQueryRunner();

    Entity entity = underlay.getEntity("conditionOccurrence");
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getIdAttribute(), false);
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute("end_date"),
            BinaryOperator.EQUALS,
            Literal.forDate(Date.valueOf("2024-01-24")));
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));

    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("date", listQueryResult.getSqlNoParams(), table);
  }

  @Test
  void timestampRemoveParam() throws IOException {
    Entity entity = underlay.getEntity("conditionOccurrence");
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getIdAttribute(), false);
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            entity.getAttribute("end_date"),
            BinaryOperator.EQUALS,
            Literal.forTimestamp(Timestamp.valueOf("2024-01-24 11:54:32")));
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));

    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("timestamp", listQueryResult.getSqlNoParams(), table);
  }

  @Test
  void overlappingParams() throws IOException {
    Entity entity = underlay.getPrimaryEntity();
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getIdAttribute(), false);
    List<EntityFilter> attributeFilters = new ArrayList<>();
    for (int i = 0; i < 11; i++) {
      attributeFilters.add(
          new AttributeFilter(
              underlay,
              entity,
              entity.getIdAttribute(),
              BinaryOperator.EQUALS,
              Literal.forInt64(25L + i)));
    }
    BooleanAndOrFilter booleanAndOrFilter =
        new BooleanAndOrFilter(BooleanAndOrFilter.LogicalOperator.OR, attributeFilters);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), booleanAndOrFilter, null, null));

    assertTrue(listQueryResult.getSql().contains("val1"));
    assertTrue(listQueryResult.getSql().contains("val10"));

    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("overlapping", listQueryResult.getSqlNoParams(), table);
  }
}
