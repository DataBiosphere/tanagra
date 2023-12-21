package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.query.sql.SqlTable;
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

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService(getServiceConfigName());
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
    bqQueryRunner =
        new BQQueryRunner(szService.bigQuery.queryProjectId, szService.bigQuery.dataLocation);
  }

  protected String getServiceConfigName() {
    return "cmssynpuf_broad";
  }

  protected void assertSqlMatchesWithTableNameOnly(String testName, String sql, SqlTable... tables)
      throws IOException {
    String sqlWrittenToFile = sql;
    for (SqlTable table : tables) {
      sqlWrittenToFile =
          sqlWrittenToFile.replace(table.renderSQL(), "${" + table.getTableName() + "}");
    }
    GeneratedSqlUtils.checkMatchesOrOverwriteGoldenFile(
        SqlFormatter.format(sqlWrittenToFile),
        "sql/" + this.getClass().getSimpleName() + "/" + testName + ".sql");
  }
}
