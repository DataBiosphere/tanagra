package bio.terra.tanagra.service.accesscontrol;

import bio.terra.common.exception.UnauthorizedException;
import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.authentication.UserId;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccessControlService {
  // The application configuration specifies which AccessControl implementation class this service
  // calls.
  private final AccessControl accessControlImpl;

  @Autowired
  public AccessControlService(AccessControlConfiguration accessControlConfiguration) {
    AccessControl accessControlImplInstance =
        accessControlConfiguration.getModel().createNewInstance();
    accessControlImplInstance.initialize(
        accessControlConfiguration.getParams(),
        accessControlConfiguration.getBasePath(),
        accessControlConfiguration.getOauthClientId());

    this.accessControlImpl = accessControlImplInstance;
  }

  public void throwIfUnauthorized(UserId user, Permissions permissions) {
    throwIfUnauthorized(user, permissions, null);
  }

  public void throwIfUnauthorized(
      UserId user, Permissions permissions, @Nullable ResourceId resource) {
    if (!isAuthorized(user, permissions, resource)) {
      throw new UnauthorizedException(
          "User is unauthorized to "
              + permissions.logString()
              + " "
              + (resource == null ? "" : resource.getType() + ":" + resource.getId()));
    }
  }

  public boolean isAuthorized(UserId user, Permissions permissions, ResourceId resource) {
    if (user == null) {
      throw new SystemException("Invalid user");
    } else if (resource != null && !permissions.getType().equals(resource.getType())) {
      throw new SystemException(
          "Permissions and resource types do not match: "
              + permissions.getType()
              + ", "
              + resource.getType());
    }
    return accessControlImpl.isAuthorized(user, permissions, resource);
  }

  public ResourceCollection listAuthorizedResources(
      UserId user, Permissions permissions, ResourceId parentResource) {
    return listAuthorizedResources(user, permissions, parentResource, 0, Integer.MAX_VALUE);
  }

  public ResourceCollection listAuthorizedResources(UserId user, Permissions permissions) {
    return listAuthorizedResources(user, permissions, 0, Integer.MAX_VALUE);
  }

  public ResourceCollection listAuthorizedResources(
      UserId user, Permissions permissions, int offset, int limit) {
    return listAuthorizedResources(user, permissions, null, offset, limit);
  }

  public ResourceCollection listAuthorizedResources(
      UserId user,
      Permissions permissions,
      @Nullable ResourceId parentResource,
      int offset,
      int limit) {
    return accessControlImpl.listAuthorizedResources(
        user, permissions, parentResource, offset, limit);
  }

  public Permissions getPermissions(UserId user, ResourceId resource) {
    return accessControlImpl.getPermissions(user, resource);
  }

  public ResourceCollection listAllPermissions(
      UserId user, ResourceType type, @Nullable ResourceId parentResource, int offset, int limit) {
    if (type.hasParentResourceType() && parentResource == null) {
      throw new SystemException(
          "Parent resource must be specified when listing resources of type: " + type);
    }
    return accessControlImpl.listAllPermissions(user, type, parentResource, offset, limit);
  }
}
