package bio.terra.tanagra.api.sdd;

import bio.terra.tanagra.api.omop.OmopNoteTest;

public class SddNoteTest extends OmopNoteTest {
  @Override
  protected String getUnderlayName() {
    return "sdd";
  }
}
