package bio.terra.tanagra.service.accesscontrol.model;

import bio.terra.tanagra.service.accesscontrol.model.impl.AouWorkbenchAccessControl;
import bio.terra.tanagra.service.accesscontrol.model.impl.OpenAccessControl;
import bio.terra.tanagra.service.accesscontrol.model.impl.OpenUnderlayUserPrivateStudyAccessControl;
import bio.terra.tanagra.service.accesscontrol.model.impl.VerilyGroupsAccessControl;
import bio.terra.tanagra.service.accesscontrol.model.impl.VumcAdminAccessControl;
import java.util.function.Supplier;

public enum CoreModel {
  OPEN_ACCESS(OpenAccessControl::new),
  OPEN_UNDERLAY_USER_PRIVATE_STUDY(OpenUnderlayUserPrivateStudyAccessControl::new),
  VUMC_ADMIN(VumcAdminAccessControl::new),
  VERILY_GROUP(VerilyGroupsAccessControl::new),
  AOU_WORKBENCH(AouWorkbenchAccessControl::new);

  private final Supplier<FineGrainedAccessControl> createNewInstanceFn;

  CoreModel(Supplier<FineGrainedAccessControl> createNewInstanceFn) {
    this.createNewInstanceFn = createNewInstanceFn;
  }

  public FineGrainedAccessControl createNewInstance() {
    return createNewInstanceFn.get();
  }
}
