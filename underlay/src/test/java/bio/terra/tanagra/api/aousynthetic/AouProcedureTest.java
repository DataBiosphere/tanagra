package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopProcedureTest;

public class AouProcedureTest extends OmopProcedureTest {
  @Override
  protected String getUnderlayName() {
    return "aouSR2019q4r4";
  }
}
