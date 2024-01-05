package bio.terra.tanagra.query.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test the generic SQL generation functions that we expect to apply to all SQL dialects. These are
 * the functions that are implemented as defaults in {@link
 * bio.terra.tanagra.query.sql.translator.ApiTranslator}.
 */
public class SqlQueryFieldTest {
  @Test
  void fieldNoAlias() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");

    // SELECT
    String selectSql = SqlQueryField.of(field).renderForSelect(tableAlias);
    assertEquals("tableAlias.columnName", selectSql);

    // ORDER BY
    String orderBySql = SqlQueryField.of(field).renderForOrderBy(tableAlias, false);
    assertEquals("tableAlias.columnName", orderBySql);
    orderBySql = SqlQueryField.of(field).renderForOrderBy(tableAlias, true);
    assertEquals("columnName", orderBySql);
  }

  @Test
  void fieldNoTableAlias() {
    SqlField field = SqlField.of("columnName");

    // SELECT
    String selectSql = SqlQueryField.of(field).renderForSelect();
    assertEquals("columnName", selectSql);

    // ORDER BY
    String orderBySql = SqlQueryField.of(field).renderForOrderBy(null, false);
    assertEquals("columnName", orderBySql);
    orderBySql = SqlQueryField.of(field).renderForOrderBy(null, true);
    assertEquals("columnName", orderBySql);
  }

  @Test
  void fieldNoFn() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName");
    String fieldAlias = "fieldAlias";

    // SELECT
    String selectSql = SqlQueryField.of(field, fieldAlias).renderForSelect(tableAlias);
    assertEquals("tableAlias.columnName AS fieldAlias", selectSql);

    // ORDER BY
    String orderBySql = SqlQueryField.of(field, fieldAlias).renderForOrderBy(tableAlias, false);
    assertEquals("tableAlias.columnName", orderBySql);
    orderBySql = SqlQueryField.of(field, fieldAlias).renderForOrderBy(tableAlias, true);
    assertEquals("fieldAlias", orderBySql);
  }

  @Test
  void fieldSimpleFn() {
    String tableAlias = "tableAlias";
    SqlField field = SqlField.of("columnName", "MAX");
    String fieldAlias = "fieldAlias";

    // SELECT
    String selectSql = SqlQueryField.of(field, fieldAlias).renderForSelect(tableAlias);
    assertEquals("MAX(tableAlias.columnName) AS fieldAlias", selectSql);

    // ORDER BY
    String orderBySql = SqlQueryField.of(field, fieldAlias).renderForOrderBy(tableAlias, false);
    assertEquals("MAX(tableAlias.columnName)", orderBySql);
    orderBySql = SqlQueryField.of(field, fieldAlias).renderForOrderBy(tableAlias, true);
    assertEquals("fieldAlias", orderBySql);
  }

  @Test
  void fieldSubstFn() {
    String tableAlias = "tableAlias";
    SqlField field =
        SqlField.of(
            "columnName",
            "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), ${fieldSql}, DAY) / 365.25) AS INT64)");
    String fieldAlias = "fieldAlias";

    // SELECT
    String selectSql = SqlQueryField.of(field, fieldAlias).renderForSelect(tableAlias);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64) AS fieldAlias",
        selectSql);

    // ORDER BY
    String orderBySql = SqlQueryField.of(field, fieldAlias).renderForOrderBy(tableAlias, false);
    assertEquals(
        "CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), tableAlias.columnName, DAY) / 365.25) AS INT64)",
        orderBySql);
    orderBySql = SqlQueryField.of(field, fieldAlias).renderForOrderBy(tableAlias, true);
    assertEquals("fieldAlias", orderBySql);
  }
}
