package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopObservationTest;

public class AouObservationTest extends OmopObservationTest {
  @Override
  protected String getUnderlayName() {
    return "aouSR2019q4r4";
  }
}
