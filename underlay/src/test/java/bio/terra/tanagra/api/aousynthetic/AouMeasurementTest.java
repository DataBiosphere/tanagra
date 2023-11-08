package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopMeasurementTest;

public class AouMeasurementTest extends OmopMeasurementTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }
}
