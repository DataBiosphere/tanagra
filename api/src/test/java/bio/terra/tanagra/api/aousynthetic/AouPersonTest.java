package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopPersonTest;

public class AouPersonTest extends OmopPersonTest {
  @Override
  protected String getUnderlayName() {
    return "aou_synthetic";
  }
}
