package bio.terra.tanagra.service.identity;

import bio.terra.tanagra.plugin.Plugin;
import bio.terra.tanagra.service.accesscontrol.UserId;

public interface IdentityPlugin extends Plugin {
  UserId getUserId(Object credential);
}
