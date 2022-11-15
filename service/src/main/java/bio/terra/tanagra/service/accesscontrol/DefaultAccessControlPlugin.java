package bio.terra.tanagra.service.accesscontrol;

import javax.annotation.Nullable;

/**
 * Default access control plugin implementation that allows everything: all actions, listing all
 * resources.
 */
public class DefaultAccessControlPlugin implements AccessControlPlugin {
  @Override
  public String getName() {
    return "DEFAULT_ACCESS_CONTROL_PLUGIN";
  }

  @Override
  public boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, @Nullable ResourceId resourceId) {
    // Every possible action is allowed.
    return true;
  }

  @Override
  public ResourceIdCollection listResourceIds(ResourceType type, int offset, int limit) {
    // Everyone can list everything.
    return ResourceIdCollection.allResourceIds();
  }

  @Override
  public UserId getUserId(Object credential) {
    // TODO: Decode a GoogleCredential object here to get the user email/id.
    return new UserId("single_user");
  }
}
