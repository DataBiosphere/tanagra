package bio.terra.tanagra.api.cmssynpuf;

public class ProcedureQueriesTest extends bio.terra.tanagra.api.omop.ProcedureQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
