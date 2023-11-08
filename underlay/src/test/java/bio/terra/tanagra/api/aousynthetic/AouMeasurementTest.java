package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopMeasurementTest;

public class AouMeasurementTest extends OmopMeasurementTest {
  @Override
  protected String getUnderlayName() {
    return "aouSR2019q4r4";
  }
}
