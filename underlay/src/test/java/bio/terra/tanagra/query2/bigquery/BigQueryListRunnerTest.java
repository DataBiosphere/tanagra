package bio.terra.tanagra.query2.bigquery;

import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.utils.SqlFormatter;
import java.io.IOException;

public abstract class BigQueryListRunnerTest {
  protected void assertSqlMatchesWithTableNameOnly(
      String testName, String sql, TablePointer... tables) throws IOException {
    for (TablePointer table : tables) {
      sql = sql.replace(table.renderSQL(), "${" + table.getTableName() + "}");
    }
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        SqlFormatter.format(sql),
        "sql/" + this.getClass().getSimpleName() + "/" + testName + ".sql");
  }
}
