package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopDeviceTest;

public class CmsDeviceTest extends OmopDeviceTest {
  @Override
  protected String getUnderlayName() {
    return "cmssynpuf";
  }
}
