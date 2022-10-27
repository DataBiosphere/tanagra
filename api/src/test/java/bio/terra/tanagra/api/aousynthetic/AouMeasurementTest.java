package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopMeasurementTest;

public class AouMeasurementTest extends OmopMeasurementTest {
  @Override
  protected String getUnderlayName() {
    return "aou_synthetic";
  }
}
