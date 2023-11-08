package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopObservationTest;

public class CmsObservationTest extends OmopObservationTest {
  @Override
  protected String getUnderlayName() {
    return "cmssynpuf";
  }
}
