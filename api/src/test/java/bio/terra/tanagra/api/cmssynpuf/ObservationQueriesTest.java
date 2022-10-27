package bio.terra.tanagra.api.cmssynpuf;

public class ObservationQueriesTest extends bio.terra.tanagra.api.omop.ObservationQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
