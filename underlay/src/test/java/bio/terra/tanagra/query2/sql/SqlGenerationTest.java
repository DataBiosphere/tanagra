package bio.terra.tanagra.query2.sql;

import static bio.terra.tanagra.query2.sql.SqlGeneration.binaryFilterSql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.booleanAndOrFilterSql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.booleanNotFilterSql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.functionFilterSql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.havingSql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.inSelectFilterSql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.orderBySql;
import static bio.terra.tanagra.query2.sql.SqlGeneration.selectSql;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class SqlGenerationTest {
  @Test
  void fieldNoAlias() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    String tableAlias = "tableAlias";
    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();

    // SELECT
    String selectSql = selectSql(SqlField.of(field, null), tableAlias);
    assertEquals("tableAlias.columnName", selectSql);

    // ORDER BY
    String orderBySql = orderBySql(SqlField.of(field, null), tableAlias, false);
    assertEquals("tableAlias.columnName", orderBySql);
    orderBySql = orderBySql(SqlField.of(field, null), tableAlias, true);
    assertEquals("columnName", orderBySql);
  }

  @Test
  void fieldNoTableAlias() {
    TablePointer table = new TablePointer("projectId", "datasetId", "tableName");
    FieldPointer field =
        new FieldPointer.Builder().tablePointer(table).columnName("columnName").build();

    // SELECT
    String selectSql = selectSql(SqlField.of(field, null), null);
    assertEquals("columnName", selectSql);

    // ORDER BY
    String orderBySql = orderBySql(SqlField.of(field, null), null, false);
    assertEquals("columnName", orderBySql);
    orderBySql = orderBySql(SqlField.of(field, null), null, true);
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
    String selectSql = selectSql(SqlField.of(field, fieldAlias), tableAlias);
    assertEquals("tableAlias.columnName AS fieldAlias", selectSql);

    // ORDER BY
    String orderBySql = orderBySql(SqlField.of(field, fieldAlias), tableAlias, false);
    assertEquals("tableAlias.columnName", orderBySql);
    orderBySql = orderBySql(SqlField.of(field, fieldAlias), tableAlias, true);
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
    String selectSql = selectSql(SqlField.of(field, fieldAlias), tableAlias);
    assertEquals("MAX(tableAlias.columnName) AS fieldAlias", selectSql);

    // ORDER BY
    String orderBySql = orderBySql(SqlField.of(field, fieldAlias), tableAlias, false);
    assertEquals("MAX(tableAlias.columnName)", orderBySql);
    orderBySql = orderBySql(SqlField.of(field, fieldAlias), tableAlias, true);
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
    String selectSql = selectSql(SqlField.of(field, fieldAlias), tableAlias);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64) AS fieldAlias",
        selectSql);

    // ORDER BY
    String orderBySql = orderBySql(SqlField.of(field, fieldAlias), tableAlias, false);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64)",
        orderBySql);
    orderBySql = orderBySql(SqlField.of(field, fieldAlias), tableAlias, true);
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
        binaryFilterSql(
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
        binaryFilterSql(
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
        functionFilterSql(
            field, "REGEXP_CONTAINS(${fieldSql},${values})", List.of(val), tableAlias, sqlParams);
    assertEquals("REGEXP_CONTAINS(tableAlias.columnName,@val)", sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());

    sqlParams = new SqlParams();
    Literal val1 = new Literal(25);
    Literal val2 = new Literal(26);
    sql =
        functionFilterSql(
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
        binaryFilterSql(
            joinField, BinaryFilterVariable.BinaryOperator.EQUALS, val, null, sqlParams);
    String sql =
        inSelectFilterSql(field, tableAlias, joinField, joinTable, joinFilterSql, sqlParams);
    assertEquals(
        "tableAlias.columnName IN (SELECT joinColumnName FROM `projectId.datasetId`.joinTableName WHERE joinColumnName = @val)",
        sql);
    assertEquals(ImmutableMap.of("val", val), sqlParams.getParams());

    // With literals.
    sqlParams = new SqlParams();
    joinFilterSql =
        binaryFilterSql(
            joinField, BinaryFilterVariable.BinaryOperator.EQUALS, val, null, sqlParams);
    Literal val0 = new Literal(24);
    Literal val1 = new Literal(25);
    sql =
        inSelectFilterSql(
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
        binaryFilterSql(
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
        binaryFilterSql(
            joinField, BinaryFilterVariable.BinaryOperator.EQUALS, val2, null, sqlParams);
    String filterSql2 =
        inSelectFilterSql(field, tableAlias, joinField, joinTable, joinFilterSql, sqlParams);

    String sql =
        booleanAndOrFilterSql(
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
        binaryFilterSql(
            field,
            BinaryFilterVariable.BinaryOperator.LESS_THAN_OR_EQUAL,
            val,
            tableAlias,
            sqlParams);
    String sql = booleanNotFilterSql(subFilterSql);
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
        havingSql(
            BinaryFilterVariable.BinaryOperator.GREATER_THAN_OR_EQUAL,
            groupByCount.getInt64Val().intValue(),
            groupByField,
            tableAlias,
            sqlParams);
    assertEquals("GROUP BY tableAlias.columnName HAVING COUNT(*) >= @groupByCount", havingSql);
    assertEquals(ImmutableMap.of("groupByCount", groupByCount), sqlParams.getParams());
  }
}
