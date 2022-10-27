package bio.terra.tanagra.api.cmssynpuf;

public class ObservationOccurrenceQueriesTest
    extends bio.terra.tanagra.api.omop.ObservationOccurrenceQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
