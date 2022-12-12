package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.plugin.Plugin;
import bio.terra.tanagra.service.auth.UserId;

/** Interface that all access control plugins must implement. */
public interface AccessControlPlugin extends Plugin {
  boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, ResourceId resourceId);

  ResourceIdCollection listResourceIds(UserId userId, ResourceType type, int offset, int limit);
}
