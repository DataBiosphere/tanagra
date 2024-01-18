package bio.terra.tanagra.query.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.NaryOperator;
import bio.terra.tanagra.api.shared.UnaryOperator;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.bigquery.translator.BQApiTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the generic SQL generation functions that we expect to apply to all SQL dialects. These are
 * the functions that are implemented as defaults in {@link
 * bio.terra.tanagra.query.sql.translator.ApiTranslator}.
 */
public class ApiTranslatorTest {
  private ApiTranslator apiTranslator;

  @BeforeEach
  void createTranslator() {
    apiTranslator = new BQApiTranslator();
  }

  @Test
  void unaryFilter() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");
    SqlParams sqlParams = new SqlParams();
    String sql =
        apiTranslator.unaryFilterSql(field, UnaryOperator.IS_EMPTY_STRING, tableAlias, sqlParams);
    assertEquals("tableAlias.columnName = ''", sql);
    assertEquals(ImmutableMap.of(), sqlParams.getParams());
  }

  @Test
  void binaryFilter() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");
    SqlParams sqlParams = new SqlParams();
    Literal val = Literal.forTimestamp(Timestamp.from(Instant.now()));
    String sql =
        apiTranslator.binaryFilterSql(
            field, BinaryOperator.LESS_THAN_OR_EQUAL, val, tableAlias, sqlParams);
    assertEquals("tableAlias.columnName <= @val", sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());

    field =
        SqlField.of(
            "columnName",
            "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), ${fieldSql}, DAY) / 365.25) AS INT64)");
    sqlParams = new SqlParams();
    val = Literal.forInt64(25L);
    sql =
        apiTranslator.binaryFilterSql(
            field, BinaryOperator.GREATER_THAN, val, tableAlias, sqlParams);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64) > @val",
        sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());
  }

  @Test
  void textSearchFilter() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");
    SqlParams sqlParams = new SqlParams();
    Literal val = Literal.forString("textToSearch");
    String sql =
        apiTranslator.textSearchFilterSql(
            field, TextSearchFilter.TextSearchOperator.EXACT_MATCH, val, tableAlias, sqlParams);
    assertEquals("REGEXP_CONTAINS(UPPER(tableAlias.columnName), UPPER(@val))", sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());
  }

  @Test
  void naryFilter() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");
    SqlParams sqlParams = new SqlParams();
    Literal val1 = Literal.forInt64(25L);
    Literal val2 = Literal.forInt64(26L);
    Literal val3 = Literal.forInt64(27L);
    String sql =
        apiTranslator.naryFilterSql(
            field, NaryOperator.IN, List.of(val1, val2, val3), tableAlias, sqlParams);
    assertEquals("tableAlias.columnName IN (@val,@val0,@val1)", sql);
    assertEquals(ImmutableMap.of("val", val1, "val0", val2, "val1", val3), sqlParams.getParams());

    sqlParams = new SqlParams();
    sql =
        apiTranslator.naryFilterSql(
            field, NaryOperator.NOT_IN, List.of(val1, val2, val3), tableAlias, sqlParams);
    assertEquals("tableAlias.columnName NOT IN (@val,@val0,@val1)", sql);
    assertEquals(ImmutableMap.of("val", val1, "val0", val2, "val1", val3), sqlParams.getParams());

    sqlParams = new SqlParams();
    sql =
        apiTranslator.naryFilterSql(
            field, NaryOperator.BETWEEN, List.of(val1, val2), tableAlias, sqlParams);
    assertEquals("tableAlias.columnName BETWEEN @val AND @val0", sql);
    assertEquals(ImmutableMap.of("val", val1, "val0", val2), sqlParams.getParams());

    assertThrows(
        InvalidQueryException.class,
        () ->
            apiTranslator.naryFilterSql(
                field,
                NaryOperator.BETWEEN,
                List.of(val1, val2, val3),
                tableAlias,
                new SqlParams()));
  }

  @Test
  void functionWithCommaSeparatedArgsFilter() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");
    SqlParams sqlParams = new SqlParams();
    Literal val = Literal.forString("test");
    String sql =
        apiTranslator.functionWithCommaSeparatedArgsFilterSql(
            field, "REGEXP_CONTAINS(${fieldSql},${values})", List.of(val), tableAlias, sqlParams);
    assertEquals("REGEXP_CONTAINS(tableAlias.columnName,@val)", sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());

    sqlParams = new SqlParams();
    Literal val1 = Literal.forInt64(25L);
    Literal val2 = Literal.forInt64(26L);
    sql =
        apiTranslator.functionWithCommaSeparatedArgsFilterSql(
            field, "${fieldSql} IN (${values})", List.of(val1, val2), tableAlias, sqlParams);
    assertEquals("tableAlias.columnName IN (@val,@val0)", sql);
    assertEquals(ImmutableMap.of("val", val1, "val0", val2), sqlParams.getParams());
  }

  @Test
  void inSelectFilter() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");
    BQTable joinTable = new BQTable("projectId", "datasetId", "joinTableName");
    SqlField joinField = SqlField.of("joinColumnName");

    // Without literals.
    SqlParams sqlParams = new SqlParams();
    Literal val = Literal.forInt64(14L);
    String joinFilterSql =
        apiTranslator.binaryFilterSql(joinField, BinaryOperator.EQUALS, val, null, sqlParams);
    String sql =
        apiTranslator.inSelectFilterSql(
            field, tableAlias, joinField, joinTable, joinFilterSql, null, sqlParams);
    assertEquals(
        "tableAlias.columnName IN (SELECT joinColumnName FROM `projectId.datasetId`.joinTableName WHERE joinColumnName = @val)",
        sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());

    // With literals.
    sqlParams = new SqlParams();
    joinFilterSql =
        apiTranslator.binaryFilterSql(joinField, BinaryOperator.EQUALS, val, null, sqlParams);
    Literal val0 = Literal.forInt64(24L);
    Literal val1 = Literal.forInt64(25L);
    sql =
        apiTranslator.inSelectFilterSql(
            field, tableAlias, joinField, joinTable, joinFilterSql, null, sqlParams, val0, val1);
    assertEquals(
        "tableAlias.columnName IN (SELECT joinColumnName FROM `projectId.datasetId`.joinTableName WHERE joinColumnName = @val UNION ALL SELECT @val0 UNION ALL SELECT @val1)",
        sql);
    assertEquals(ImmutableMap.of("val", val, "val0", val0, "val1", val1), sqlParams.getParams());

    // Null sub-filter, without having clause.
    sqlParams = new SqlParams();
    val = Literal.forInt64(38L);
    sql =
        apiTranslator.inSelectFilterSql(
            field, tableAlias, joinField, joinTable, null, null, sqlParams, val);
    assertEquals(
        "tableAlias.columnName IN (SELECT joinColumnName FROM `projectId.datasetId`.joinTableName UNION ALL SELECT @val)",
        sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());

    // Null sub-filter, with having clause.
    sqlParams = new SqlParams();
    sql =
        apiTranslator.inSelectFilterSql(
            field,
            tableAlias,
            joinField,
            joinTable,
            null,
            "GROUP BY joinColumnName HAVING COUNT(*) > 1",
            sqlParams);
    assertEquals(
        "tableAlias.columnName IN (SELECT joinColumnName FROM `projectId.datasetId`.joinTableName GROUP BY joinColumnName HAVING COUNT(*) > 1)",
        sql);
  }

  @Test
  void booleanAndOrFilter() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");
    SqlParams sqlParams = new SqlParams();

    Literal val1 = Literal.forTimestamp(Timestamp.from(Instant.now()));
    String filterSql1 =
        apiTranslator.binaryFilterSql(
            field, BinaryOperator.LESS_THAN_OR_EQUAL, val1, tableAlias, sqlParams);

    BQTable joinTable = new BQTable("projectId", "datasetId", "joinTableName");
    SqlField joinField = SqlField.of("joinColumnName");
    Literal val2 = Literal.forInt64(14L);
    String joinFilterSql =
        apiTranslator.binaryFilterSql(joinField, BinaryOperator.EQUALS, val2, null, sqlParams);
    String filterSql2 =
        apiTranslator.inSelectFilterSql(
            field, tableAlias, joinField, joinTable, joinFilterSql, null, sqlParams);

    String sql =
        apiTranslator.booleanAndOrFilterSql(
            BooleanAndOrFilter.LogicalOperator.OR, filterSql1, filterSql2);
    assertEquals(
        "(tableAlias.columnName <= @val) OR (tableAlias.columnName IN (SELECT joinColumnName FROM `projectId.datasetId`.joinTableName WHERE joinColumnName = @val0))",
        sql);
    assertEquals(ImmutableMap.of("val", val1, "val0", val2), sqlParams.getParams());
  }

  @Test
  void booleanNotFilter() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");
    SqlParams sqlParams = new SqlParams();

    Literal val = Literal.forTimestamp(Timestamp.from(Instant.now()));
    String subFilterSql =
        apiTranslator.binaryFilterSql(
            field, BinaryOperator.LESS_THAN_OR_EQUAL, val, tableAlias, sqlParams);
    String sql = apiTranslator.booleanNotFilterSql(subFilterSql);
    assertEquals("NOT tableAlias.columnName <= @val", sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());
  }

  @Test
  void having() {
    String tableAlias = "tableAlias";
    SqlField groupByField = SqlField.of("columnName");
    SqlParams sqlParams = new SqlParams();
    Literal groupByCount = Literal.forInt64(4L);

    String havingSql =
        apiTranslator.havingSql(
            BinaryOperator.GREATER_THAN_OR_EQUAL,
            groupByCount.getInt64Val().intValue(),
            List.of(groupByField),
            tableAlias,
            sqlParams);
    assertEquals("GROUP BY tableAlias.columnName HAVING COUNT(*) >= @groupByCount", havingSql);
    assertEquals(ImmutableMap.of("groupByCount", groupByCount), sqlParams.getParams());
  }
}
