package bio.terra.tanagra.api.aou;

import bio.terra.tanagra.api.omop.OmopConditionTest;

public class AouConditionTest extends OmopConditionTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }
}