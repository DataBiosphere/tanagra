package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.testing.GeneratedSqlUtils;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.serialization.SZService;
import bio.terra.tanagra.underlay.serialization.SZUnderlay;
import bio.terra.tanagra.utils.SqlFormatter;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;

public abstract class BQRunnerTest {
  protected Underlay underlay;
  protected BQQueryRunner bqQueryRunner;
  private SZService szService;

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    szService = configReader.readService(getServiceConfigName());
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
    bqQueryRunner = (BQQueryRunner) underlay.getQueryRunner();
  }

  protected String getServiceConfigName() {
    return "cmssynpuf_broad";
  }

  protected void assertSqlMatchesWithTableNameOnly(String testName, String sql, BQTable... tables)
      throws IOException {
    String sqlWrittenToFile = sql;
    for (BQTable table : tables) {
      String tableNameWithoutPrefix = table.getTableName();
      if (szService.bigQuery.indexData.tablePrefix != null) {
        tableNameWithoutPrefix =
            tableNameWithoutPrefix.replaceFirst(
                '^' + szService.bigQuery.indexData.tablePrefix + '_', "");
      }
      sqlWrittenToFile =
          sqlWrittenToFile.replace(table.render(), "${" + tableNameWithoutPrefix + "}");
    }
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        SqlFormatter.format(sqlWrittenToFile),
        "sql/" + this.getClass().getSimpleName() + "/" + testName + ".sql");
  }
}
