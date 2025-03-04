package bio.terra.tanagra.query.bigquery.sqlbuilding.filter;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.query.list.ListQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.query.bigquery.BQRunnerTest;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BQAttributeFilterTest extends BQRunnerTest {
  @Test
  void attributeFilter() throws IOException {
    // Filter with unary operator.
    Entity entity = underlay.getPrimaryEntity();
    Attribute attribute = entity.getAttribute("ethnicity");
    AttributeFilter attributeFilter =
        new AttributeFilter(underlay, entity, attribute, UnaryOperator.IS_NOT_NULL);
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("year_of_birth"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly("attributeFilterUnary", listQueryResult.getSql(), table);

    // Filter with binary operator.
    attribute = entity.getAttribute("year_of_birth");
    attributeFilter =
        new AttributeFilter(
            underlay, entity, attribute, BinaryOperator.NOT_EQUALS, Literal.forInt64(1_956L));
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));
    assertSqlMatchesWithTableNameOnly("attributeFilterBinary", listQueryResult.getSql(), table);

    // Filter with n-ary operator IN.
    attribute = entity.getAttribute("age");
    attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            NaryOperator.IN,
            List.of(Literal.forInt64(18L), Literal.forInt64(19L), Literal.forInt64(20L)));
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));
    assertSqlMatchesWithTableNameOnly("attributeFilterNaryIn", listQueryResult.getSql(), table);

    // Filter with n-ary operator BETWEEN.
    attribute = entity.getAttribute("age");
    attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            NaryOperator.BETWEEN,
            List.of(Literal.forInt64(45L), Literal.forInt64(65L)));
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));
    assertSqlMatchesWithTableNameOnly(
        "attributeFilterNaryBetween", listQueryResult.getSql(), table);
  }

  @Test
  void repeatedAttributeFilter() throws IOException {
    Entity entity = underlay.getEntity("condition");

    // We don't have an example of an attribute with a repeated data type, yet.
    // So create an artificial attribute just for this test.
    Attribute attribute =
        new Attribute(
            "vocabulary",
            DataType.STRING,
            true,
            false,
            false,
            "['foo', 'bar', 'baz', ${fieldSql}]",
            DataType.STRING,
            entity.getAttribute("vocabulary").isComputeDisplayHint(),
            entity.getAttribute("vocabulary").getEmptyValueDisplay(),
            entity.getAttribute("vocabulary").isSuppressedForExport(),
            entity.getAttribute("vocabulary").isVisitDateForTemporalQuery(),
            entity.getAttribute("vocabulary").isVisitIdForTemporalQuery(),
            entity.getAttribute("vocabulary").getSourceQuery());

    // Filter with binary operator NOT_EQUALS.
    AttributeFilter attributeFilter =
        new AttributeFilter(
            underlay, entity, attribute, BinaryOperator.NOT_EQUALS, Literal.forString("SNOMED"));
    AttributeField simpleAttribute =
        new AttributeField(underlay, entity, entity.getAttribute("name"), false);
    ListQueryResult listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));
    BQTable table = underlay.getIndexSchema().getEntityMain(entity.getName()).getTablePointer();
    assertSqlMatchesWithTableNameOnly(
        "repeatedAttributeFilterBinaryNotEquals", listQueryResult.getSql(), table);

    // Filter with n-ary operator IN.
    attributeFilter =
        new AttributeFilter(
            underlay,
            entity,
            attribute,
            NaryOperator.IN,
            List.of(
                Literal.forString("bar"),
                Literal.forString("ICD9CM"),
                Literal.forString("SNOMED")));
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));
    assertSqlMatchesWithTableNameOnly(
        "repeatedAttributeFilterNaryIn", listQueryResult.getSql(), table);

    // Filter with unary operator IS_NULL
    attributeFilter = new AttributeFilter(underlay, entity, attribute, UnaryOperator.IS_NULL);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));
    assertSqlMatchesWithTableNameOnly(
        "repeatedAttributeFilterUnaryIsNull", listQueryResult.getSql(), table);

    // Filter with unary operator IS_NOT_NULL
    attributeFilter = new AttributeFilter(underlay, entity, attribute, UnaryOperator.IS_NOT_NULL);
    listQueryResult =
        bqQueryRunner.run(
            ListQueryRequest.dryRunAgainstIndexData(
                underlay, entity, List.of(simpleAttribute), attributeFilter, null, null));
    assertSqlMatchesWithTableNameOnly(
        "repeatedAttributeFilterUnaryIsNotNull", listQueryResult.getSql(), table);
  }
}
