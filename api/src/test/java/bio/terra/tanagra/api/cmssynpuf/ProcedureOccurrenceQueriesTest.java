package bio.terra.tanagra.api.cmssynpuf;

public class ProcedureOccurrenceQueriesTest
    extends bio.terra.tanagra.api.omop.ProcedureOccurrenceQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
