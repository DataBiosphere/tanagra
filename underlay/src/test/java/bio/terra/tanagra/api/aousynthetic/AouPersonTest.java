package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopPersonTest;

public class AouPersonTest extends OmopPersonTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }
}
