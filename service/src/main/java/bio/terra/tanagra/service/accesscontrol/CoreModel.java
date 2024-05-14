package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.service.accesscontrol.impl.AouWorkbenchAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.OpenAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.VerilyGroupsAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.VumcAdminAccessControl;
import java.util.function.Supplier;

public enum CoreModel {
  OPEN_ACCESS(OpenAccessControl::new),
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
