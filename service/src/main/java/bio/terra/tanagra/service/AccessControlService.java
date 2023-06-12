package bio.terra.tanagra.service;

import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.service.accesscontrol.AccessControl;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.UserId;
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

  public void throwIfUnauthorized(UserId userId, Action action, ResourceType resourceType) {
    throwIfUnauthorized(userId, action, resourceType, null);
  }

  public void throwIfUnauthorized(
      UserId userId, Action action, ResourceType resourceType, @Nullable ResourceId resourceId) {
    if (!isAuthorized(userId, action, resourceType, resourceId)) {
      throw new UnauthorizedException(
          "User is unauthorized to "
              + action
              + " "
              + resourceType
              + " "
              + (resourceId == null ? "" : resourceId.getId()));
    }
  }

  public boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, @Nullable ResourceId resourceId) {
    if (userId == null) {
      throw new BadRequestException("Invalid user id");
    }
    if (!resourceType.hasAction(action)) {
      throw new BadRequestException(
          "Action not available for resource type: " + action + ", " + resourceType);
    }
    return accessControlImpl.isAuthorized(userId, action, resourceType, resourceId);
  }

  public ResourceIdCollection listResourceIds(UserId userId, ResourceType type) {
    return listResourceIds(userId, type, 0, Integer.MAX_VALUE);
  }

  public ResourceIdCollection listResourceIds(
      UserId userId, ResourceType type, int offset, int limit) {
    return accessControlImpl.listResourceIds(userId, type, offset, limit);
  }

  public ResourceIdCollection listResourceIds(
      UserId userId, ResourceType type, ResourceId parentResourceId, int offset, int limit) {
    return accessControlImpl.listResourceIds(userId, type, parentResourceId, offset, limit);
  }
}
