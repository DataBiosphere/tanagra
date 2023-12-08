package bio.terra.tanagra.query2.utils;

import static bio.terra.tanagra.query2.utils.SqlGeneration.orderBySql;
import static bio.terra.tanagra.query2.utils.SqlGeneration.selectSql;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
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
    String selectSql = selectSql(Pair.of(field, null), tableAlias);
    assertEquals("tableAlias.columnName", selectSql);

    // ORDER BY
    String orderBySql = orderBySql(Pair.of(field, null), tableAlias, false);
    assertEquals("tableAlias.columnName", orderBySql);
    orderBySql = orderBySql(Pair.of(field, null), tableAlias, true);
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
    String selectSql = selectSql(Pair.of(field, fieldAlias), tableAlias);
    assertEquals("tableAlias.columnName AS fieldAlias", selectSql);

    // ORDER BY
    String orderBySql = orderBySql(Pair.of(field, fieldAlias), tableAlias, false);
    assertEquals("tableAlias.columnName", orderBySql);
    orderBySql = orderBySql(Pair.of(field, fieldAlias), tableAlias, true);
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
    String selectSql = selectSql(Pair.of(field, fieldAlias), tableAlias);
    assertEquals("MAX(tableAlias.columnName) AS fieldAlias", selectSql);

    // ORDER BY
    String orderBySql = orderBySql(Pair.of(field, fieldAlias), tableAlias, false);
    assertEquals("MAX(tableAlias.columnName)", orderBySql);
    orderBySql = orderBySql(Pair.of(field, fieldAlias), tableAlias, true);
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
    String selectSql = selectSql(Pair.of(field, fieldAlias), tableAlias);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64) AS fieldAlias",
        selectSql);

    // ORDER BY
    String orderBySql = orderBySql(Pair.of(field, fieldAlias), tableAlias, false);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64)",
        orderBySql);
    orderBySql = orderBySql(Pair.of(field, fieldAlias), tableAlias, true);
    assertEquals("fieldAlias", orderBySql);
  }
}
