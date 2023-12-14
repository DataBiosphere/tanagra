package bio.terra.tanagra.query2.bigquery;

import bio.terra.tanagra.query.TablePointer;
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

  @BeforeEach
  void setup() {
    ConfigReader configReader = ConfigReader.fromJarResources();
    SZService szService = configReader.readService(getServiceConfigName());
    SZUnderlay szUnderlay = configReader.readUnderlay(szService.underlay);
    underlay = Underlay.fromConfig(szService.bigQuery, szUnderlay, configReader);
  }

  protected String getServiceConfigName() {
    return "cmssynpuf_broad";
  }

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
