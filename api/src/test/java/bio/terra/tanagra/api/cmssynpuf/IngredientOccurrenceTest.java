package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopIngredientOccurrenceTest;

public class IngredientOccurrenceTest extends OmopIngredientOccurrenceTest {
    @Override
    protected String getUnderlayName() {
        return "cms_synpuf";
    }
}
