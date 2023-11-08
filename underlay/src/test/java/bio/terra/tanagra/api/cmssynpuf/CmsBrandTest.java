package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopBrandTest;

public class CmsBrandTest extends OmopBrandTest {
  @Override
  protected String getUnderlayName() {
    return "cmssynpuf";
  }
}
