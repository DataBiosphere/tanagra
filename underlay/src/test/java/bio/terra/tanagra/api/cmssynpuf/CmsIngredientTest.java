package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopIngredientTest;

public class CmsIngredientTest extends OmopIngredientTest {
  @Override
  protected String getServiceConfigName() {
    return "cmssynpuf_broad";
  }
}
