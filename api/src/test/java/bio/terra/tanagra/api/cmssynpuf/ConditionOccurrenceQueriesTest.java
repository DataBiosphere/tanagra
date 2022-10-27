package bio.terra.tanagra.api.cmssynpuf;

public class ConditionOccurrenceQueriesTest
    extends bio.terra.tanagra.api.omop.ConditionOccurrenceQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
