package bio.terra.tanagra.service;

import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.tanagra.plugin.PluginService;
import bio.terra.tanagra.service.accesscontrol.AccessControlPlugin;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol.UserId;
import bio.terra.tanagra.service.identity.IdentityPlugin;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccessControlService {

  private final PluginService pluginService;

  @Autowired
  public AccessControlService(PluginService pluginService) {
    this.pluginService = pluginService;
  }

  public void throwIfUnauthorized(Object credential, Action action, ResourceType resourceType) {
    throwIfUnauthorized(credential, action, resourceType, null);
  }

  public void throwIfUnauthorized(
      Object credential,
      Action action,
      ResourceType resourceType,
      @Nullable ResourceId resourceId) {
    if (!isAuthorized(credential, action, resourceType, resourceId)) {
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
      Object credential,
      Action action,
      ResourceType resourceType,
      @Nullable ResourceId resourceId) {
    UserId userId = getUserId(credential);
    if (userId == null) {
      throw new BadRequestException("Invalid user id");
    }
    if (!resourceType.hasAction(action)) {
      throw new BadRequestException(
          "Action not available for resource type: " + action + ", " + resourceType);
    }

    // TODO: specify underlay
    return pluginService
        .getPlugin("", AccessControlPlugin.class)
        .isAuthorized(userId, action, resourceType, resourceId);
  }

  public UserId getUserId(Object credential) {
    // TODO: specify underlay
    return pluginService.getPlugin("", IdentityPlugin.class).getUserId(credential);
  }

  public ResourceIdCollection listResourceIds(ResourceType type) {
    return listResourceIds(type, 0, Integer.MAX_VALUE);
  }

  public ResourceIdCollection listResourceIds(ResourceType type, int offset, int limit) {
    // TODO: specify underlay
    return pluginService
        .getPlugin("", AccessControlPlugin.class)
        .listResourceIds(type, offset, limit);
  }
}
