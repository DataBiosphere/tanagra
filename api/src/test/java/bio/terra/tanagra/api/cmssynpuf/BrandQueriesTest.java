package bio.terra.tanagra.api.cmssynpuf;

public class BrandQueriesTest extends bio.terra.tanagra.api.omop.BrandQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
