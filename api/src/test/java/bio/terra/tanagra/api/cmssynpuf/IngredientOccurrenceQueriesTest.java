package bio.terra.tanagra.api.cmssynpuf;

public class IngredientOccurrenceQueriesTest
    extends bio.terra.tanagra.api.omop.IngredientOccurrenceQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
