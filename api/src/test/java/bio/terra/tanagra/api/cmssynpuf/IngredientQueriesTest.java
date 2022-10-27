package bio.terra.tanagra.api.cmssynpuf;

public class IngredientQueriesTest extends bio.terra.tanagra.api.omop.IngredientQueriesTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
