package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopDeviceTest;

public class AouDeviceTest extends OmopDeviceTest {
  @Override
  protected String getUnderlayName() {
    return "aou_synthetic";
  }
}
