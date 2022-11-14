package bio.terra.tanagra.api.accesscontrol;

/**
 * Interface that all access control plugins must implement. In the future, we may consider moving
 * the credential decoding to a separate plugin, so deployments can override authentication and
 * authorization separately.
 */
public interface AccessControlPlugin {
  String getName();

  boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, ResourceId resourceId);

  ResourceIdCollection listResourceIds(ResourceType type, int offset, int limit);

  UserId getUserId(Object credential);
}
