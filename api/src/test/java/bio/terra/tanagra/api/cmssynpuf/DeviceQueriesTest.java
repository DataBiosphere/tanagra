package bio.terra.tanagra.api.cmssynpuf;

public class DeviceQueriesTest extends bio.terra.tanagra.api.omop.DeviceQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
