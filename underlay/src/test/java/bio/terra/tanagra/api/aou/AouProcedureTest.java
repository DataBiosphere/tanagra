package bio.terra.tanagra.api.aou;

import bio.terra.tanagra.api.omop.OmopProcedureTest;

public class AouProcedureTest extends OmopProcedureTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }
}
