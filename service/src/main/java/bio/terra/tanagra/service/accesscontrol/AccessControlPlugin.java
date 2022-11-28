package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.plugin.IPlugin;

/**
 * Interface that all access control plugins must implement.
 */
public interface AccessControlPlugin extends IPlugin {
  String getName();

  boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, ResourceId resourceId);

  ResourceIdCollection listResourceIds(ResourceType type, int offset, int limit);
}
