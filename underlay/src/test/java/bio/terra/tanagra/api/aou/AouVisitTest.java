package bio.terra.tanagra.api.aou;

import bio.terra.tanagra.api.omop.OmopVisitTest;

public class AouVisitTest extends OmopVisitTest {
  @Override
  protected String getServiceConfigName() {
    return "aouSR2019q4r4_broad";
  }
}
