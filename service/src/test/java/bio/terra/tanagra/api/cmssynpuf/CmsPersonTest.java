package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopPersonTest;

public class CmsPersonTest extends OmopPersonTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
