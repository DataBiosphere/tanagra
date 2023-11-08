package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopConditionTest;

public class CmsConditionTest extends OmopConditionTest {
  @Override
  protected String getUnderlayName() {
    return "cmssynpuf";
  }
}
