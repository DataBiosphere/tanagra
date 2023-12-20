package bio.terra.tanagra.query2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test the generic SQL generation functions that we expect to apply to all SQL dialects. These are
 * the functions that are implemented as defaults in {@link
 * bio.terra.tanagra.query2.sql.SqlTranslator}.
 */
public class SqlTranslatorTest {
  private SqlTranslator sqlTranslator;

  @BeforeEach
  void createTranslator() {
    sqlTranslator = new BQTranslator();
  }

  @Test
  void fieldNoAlias() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";
    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();

    // SELECT
    String selectSql = sqlTranslator.selectSql(SqlField.of(field, null), tableAlias);
    assertEquals("tableAlias.columnName", selectSql);

    // ORDER BY
    String orderBySql = sqlTranslator.orderBySql(SqlField.of(field, null), tableAlias, false);
    assertEquals("tableAlias.columnName", orderBySql);
    orderBySql = sqlTranslator.orderBySql(SqlField.of(field, null), tableAlias, true);
    assertEquals("columnName", orderBySql);
  }

  @Test
  void fieldNoTableAlias() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();

    // SELECT
    String selectSql = sqlTranslator.selectSql(SqlField.of(field, null), null);
    assertEquals("columnName", selectSql);

    // ORDER BY
    String orderBySql = sqlTranslator.orderBySql(SqlField.of(field, null), null, false);
    assertEquals("columnName", orderBySql);
    orderBySql = sqlTranslator.orderBySql(SqlField.of(field, null), null, true);
    assertEquals("columnName", orderBySql);
  }

  @Test
  void fieldNoFn() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";
    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();
    String fieldAlias = "fieldAlias";

    // SELECT
    String selectSql = sqlTranslator.selectSql(SqlField.of(field, fieldAlias), tableAlias);
    assertEquals("tableAlias.columnName AS fieldAlias", selectSql);

    // ORDER BY
    String orderBySql = sqlTranslator.orderBySql(SqlField.of(field, fieldAlias), tableAlias, false);
    assertEquals("tableAlias.columnName", orderBySql);
    orderBySql = sqlTranslator.orderBySql(SqlField.of(field, fieldAlias), tableAlias, true);
    assertEquals("fieldAlias", orderBySql);
  }

  @Test
  void fieldSimpleFn() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";
    FieldPointer field =
        new FieldPointer.Builder()
            .tablePointer(table)
            .columnName("columnName")
            .sqlFunctionWrapper("MAX")
            .build();
    String fieldAlias = "fieldAlias";

    // SELECT
    String selectSql = sqlTranslator.selectSql(SqlField.of(field, fieldAlias), tableAlias);
    assertEquals("MAX(tableAlias.columnName) AS fieldAlias", selectSql);

    // ORDER BY
    String orderBySql = sqlTranslator.orderBySql(SqlField.of(field, fieldAlias), tableAlias, false);
    assertEquals("MAX(tableAlias.columnName)", orderBySql);
    orderBySql = sqlTranslator.orderBySql(SqlField.of(field, fieldAlias), tableAlias, true);
    assertEquals("fieldAlias", orderBySql);
  }

  @Test
  void fieldSubstFn() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";
    FieldPointer field =
        new FieldPointer.Builder()
            .tablePointer(table)
            .columnName("columnName")
            .sqlFunctionWrapper(
                "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), ${fieldSql}, DAY) / 365.25) AS INT64)")
            .build();
    String fieldAlias = "fieldAlias";

    // SELECT
    String selectSql = sqlTranslator.selectSql(SqlField.of(field, fieldAlias), tableAlias);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64) AS fieldAlias",
        selectSql);

    // ORDER BY
    String orderBySql = sqlTranslator.orderBySql(SqlField.of(field, fieldAlias), tableAlias, false);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64)",
        orderBySql);
    orderBySql = sqlTranslator.orderBySql(SqlField.of(field, fieldAlias), tableAlias, true);
    assertEquals("fieldAlias", orderBySql);
  }

  @Test
  void binaryFilter() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";

    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();
    SqlParams sqlParams = new SqlParams();
    Literal val = Literal.forTimestamp(Timestamp.from(Instant.now()));
    String sql =
        sqlTranslator.binaryFilterSql(
            field,
            BinaryFilterVariable.BinaryOperator.LESS_THAN_OR_EQUAL,
            val,
            tableAlias,
            sqlParams);
    assertEquals("tableAlias.columnName <= @val", sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());

    field =
        new FieldPointer.Builder()
            .tablePointer(table)
            .columnName("columnName")
            .sqlFunctionWrapper(
                "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), ${fieldSql}, DAY) / 365.25) AS INT64)")
            .build();
    sqlParams = new SqlParams();
    val = new Literal(25);
    sql =
        sqlTranslator.binaryFilterSql(
            field, BinaryFilterVariable.BinaryOperator.GREATER_THAN, val, tableAlias, sqlParams);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64) > @val",
        sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());
  }

  @Test
  void functionFilter() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";

    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();
    SqlParams sqlParams = new SqlParams();
    Literal val = new Literal("test");
    String sql =
        sqlTranslator.functionFilterSql(
            field, "REGEXP_CONTAINS(${fieldSql},${values})", List.of(val), tableAlias, sqlParams);
    assertEquals("REGEXP_CONTAINS(tableAlias.columnName,@val)", sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());

    sqlParams = new SqlParams();
    Literal val1 = new Literal(25);
    Literal val2 = new Literal(26);
    sql =
        sqlTranslator.functionFilterSql(
            field, "${fieldSql} IN (${values})", List.of(val1, val2), tableAlias, sqlParams);
    assertEquals("tableAlias.columnName IN (@val,@val0)", sql);
    assertEquals(ImmutableMap.of("val", val1, "val0", val2), sqlParams.getParams());
  }

  @Test
  void inSelectFilter() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";
    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();
    TablePointer joinTable = new TablePointer("projectId", "datasetId", "joinTableName");
    FieldPointer joinField =
        new FieldPointer.Builder().tablePointer(table).columnName("joinColumnName").build();

    // Without literals.
    SqlParams sqlParams = new SqlParams();
    Literal val = new Literal(14);
    String joinFilterSql =
        sqlTranslator.binaryFilterSql(
            joinField, BinaryFilterVariable.BinaryOperator.EQUALS, val, null, sqlParams);
    String sql =
        sqlTranslator.inSelectFilterSql(
            field, tableAlias, joinField, joinTable, joinFilterSql, sqlParams);
    assertEquals(
        "tableAlias.columnName IN (SELECT joinColumnName FROM `projectId.datasetId`.joinTableName WHERE joinColumnName = @val)",
        sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());

    // With literals.
    sqlParams = new SqlParams();
    joinFilterSql =
        sqlTranslator.binaryFilterSql(
            joinField, BinaryFilterVariable.BinaryOperator.EQUALS, val, null, sqlParams);
    Literal val0 = new Literal(24);
    Literal val1 = new Literal(25);
    sql =
        sqlTranslator.inSelectFilterSql(
            field, tableAlias, joinField, joinTable, joinFilterSql, sqlParams, val0, val1);
    assertEquals(
        "tableAlias.columnName IN (SELECT joinColumnName FROM `projectId.datasetId`.joinTableName WHERE joinColumnName = @val UNION ALL SELECT @val0 UNION ALL SELECT @val1)",
        sql);
    assertEquals(ImmutableMap.of("val", val, "val0", val0, "val1", val1), sqlParams.getParams());
  }

  @Test
  void booleanAndOrFilter() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";
    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();
    SqlParams sqlParams = new SqlParams();

    Literal val1 = Literal.forTimestamp(Timestamp.from(Instant.now()));
    String filterSql1 =
        sqlTranslator.binaryFilterSql(
            field,
            BinaryFilterVariable.BinaryOperator.LESS_THAN_OR_EQUAL,
            val1,
            tableAlias,
            sqlParams);

    TablePointer joinTable = new TablePointer("projectId", "datasetId", "joinTableName");
    FieldPointer joinField =
        new FieldPointer.Builder().tablePointer(table).columnName("joinColumnName").build();
    Literal val2 = new Literal(14);
    String joinFilterSql =
        sqlTranslator.binaryFilterSql(
            joinField, BinaryFilterVariable.BinaryOperator.EQUALS, val2, null, sqlParams);
    String filterSql2 =
        sqlTranslator.inSelectFilterSql(
            field, tableAlias, joinField, joinTable, joinFilterSql, sqlParams);

    String sql =
        sqlTranslator.booleanAndOrFilterSql(
            BooleanAndOrFilterVariable.LogicalOperator.OR, filterSql1, filterSql2);
    assertEquals(
        "tableAlias.columnName <= @val OR tableAlias.columnName IN (SELECT joinColumnName FROM `projectId.datasetId`.joinTableName WHERE joinColumnName = @val0)",
        sql);
    assertEquals(ImmutableMap.of("val", val1, "val0", val2), sqlParams.getParams());
  }

  @Test
  void booleanNotFilter() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";
    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();
    SqlParams sqlParams = new SqlParams();

    Literal val = Literal.forTimestamp(Timestamp.from(Instant.now()));
    String subFilterSql =
        sqlTranslator.binaryFilterSql(
            field,
            BinaryFilterVariable.BinaryOperator.LESS_THAN_OR_EQUAL,
            val,
            tableAlias,
            sqlParams);
    String sql = sqlTranslator.booleanNotFilterSql(subFilterSql);
    assertEquals("NOT tableAlias.columnName <= @val", sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());
  }

  @Test
  void having() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";
    FieldPointer groupByField =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();
    SqlParams sqlParams = new SqlParams();
    Literal groupByCount = new Literal(4);

    String havingSql =
        sqlTranslator.havingSql(
            BinaryFilterVariable.BinaryOperator.GREATER_THAN_OR_EQUAL,
            groupByCount.getInt64Val().intValue(),
            List.of(groupByField),
            tableAlias,
            sqlParams);
    assertEquals("GROUP BY tableAlias.columnName HAVING COUNT(*) >= @groupByCount", havingSql);
    assertEquals(ImmutableMap.of("groupByCount", groupByCount), sqlParams.getParams());
  }
}
