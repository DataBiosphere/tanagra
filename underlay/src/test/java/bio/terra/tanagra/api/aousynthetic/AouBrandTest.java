package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopBrandTest;

public class AouBrandTest extends OmopBrandTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }
}
