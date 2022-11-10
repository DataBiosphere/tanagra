package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopProcedureTest;

public class AouProcedureTest extends OmopProcedureTest {
  @Override
  protected String getUnderlayName() {
    return "aou_synthetic";
  }
}
