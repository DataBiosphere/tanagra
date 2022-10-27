package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopConditionOccurrenceTest;

public class ConditionOccurrenceTest extends OmopConditionOccurrenceTest {
    @Override
    protected String getUnderlayName() {
        return "cms_synpuf";
    }
}
