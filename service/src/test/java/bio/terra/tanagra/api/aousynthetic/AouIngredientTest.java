package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopIngredientTest;

public class AouIngredientTest extends OmopIngredientTest {
  @Override
  protected String getUnderlayName() {
    return "aouSR2019q4r4";
  }
}
