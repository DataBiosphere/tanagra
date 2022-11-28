package bio.terra.tanagra.service.identity;

import bio.terra.tanagra.plugin.IPlugin;
import bio.terra.tanagra.service.accesscontrol.UserId;

public interface IdentityPlugin extends IPlugin {
  UserId getUserId(Object credential);
}
