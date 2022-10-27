package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopBrandTest;

public class AouBrandTest extends OmopBrandTest {
  @Override
  protected String getUnderlayName() {
    return "aou_synthetic";
  }
}
