package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopBrandTest;

public class BrandTest extends OmopBrandTest {
  @Override
  protected String getUnderlayName() {
    return "cms_synpuf";
  }
}
