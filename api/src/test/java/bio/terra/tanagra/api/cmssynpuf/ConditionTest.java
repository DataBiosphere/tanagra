package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopConditionTest;

public class ConditionTest extends OmopConditionTest {
    @Override
    protected String getUnderlayName() {
        return "cms_synpuf";
    }
}
