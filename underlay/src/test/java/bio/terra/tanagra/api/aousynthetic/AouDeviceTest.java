package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopDeviceTest;

public class AouDeviceTest extends OmopDeviceTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }
}
