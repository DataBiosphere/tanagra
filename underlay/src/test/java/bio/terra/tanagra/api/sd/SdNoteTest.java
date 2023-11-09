package bio.terra.tanagra.api.sd;

import bio.terra.tanagra.api.omop.OmopNoteTest;

public class SdNoteTest extends OmopNoteTest {
  @Override
  protected String getServiceConfigName() {
    return "sd020230331_verily";
  }
}
