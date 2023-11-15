package bio.terra.tanagra.api.cmssynpuf;

import bio.terra.tanagra.api.omop.OmopProcedureTest;

public class CmsProcedureTest extends OmopProcedureTest {
  @Override
  protected String getServiceConfigName() {
    return "cmssynpuf_broad";
  }
}
