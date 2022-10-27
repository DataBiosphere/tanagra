package bio.terra.tanagra.api.cmssynpuf;

public class ConditionQueriesTest extends bio.terra.tanagra.api.omop.ConditionQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
