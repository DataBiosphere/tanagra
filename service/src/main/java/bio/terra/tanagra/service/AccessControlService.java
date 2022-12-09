package bio.terra.tanagra.service;

import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.OpenAccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.auth.UserId;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class AccessControlService {
  // TODO: Allow overriding the default plugin.
  private final AccessControlPlugin accessControlPlugin = new OpenAccessControlPlugin();

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
    return accessControlPlugin.isAuthorized(userId, action, resourceType, resourceId);
  }

  public ResourceIdCollection listResourceIds(UserId userId, ResourceType type) {
    return listResourceIds(userId, type, 0, Integer.MAX_VALUE);
  }

  public ResourceIdCollection listResourceIds(
      UserId userId, ResourceType type, int offset, int limit) {
    return accessControlPlugin.listResourceIds(userId, type, offset, limit);
  }
}
