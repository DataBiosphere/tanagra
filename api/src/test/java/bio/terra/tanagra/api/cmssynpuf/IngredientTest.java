package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopIngredientTest;

public class IngredientTest extends OmopIngredientTest {
    @Override
    protected String getUnderlayName() {
        return "cms_synpuf";
    }
}
