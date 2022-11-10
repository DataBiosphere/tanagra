package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopObservationTest;

public class AouObservationTest extends OmopObservationTest {
  @Override
  protected String getUnderlayName() {
    return "aou_synthetic";
  }
}
