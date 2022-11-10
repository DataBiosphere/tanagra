package bio.terra.tanagra.api.aousynthetic;

import bio.terra.tanagra.api.omop.OmopConditionTest;

public class AouConditionTest extends OmopConditionTest {
  @Override
  protected String getUnderlayName() {
    return "aou_synthetic";
  }
}
