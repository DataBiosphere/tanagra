package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopVisitTest;

public class AouVisitTest extends OmopVisitTest {
  @Override
  protected String getUnderlayName() {
    return "aou_synthetic";
  }
}