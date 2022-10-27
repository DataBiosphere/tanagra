package bio.terra.tanagra.api.cmssynpuf;

public class DeviceOccurrenceQueriesTest
    extends bio.terra.tanagra.api.omop.DeviceOccurrenceQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
