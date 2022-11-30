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
    // TODO: Update signature to take underlayName parameter once all routes have been configured to
    // require an underlay.
    String underlayName = "";

    if (!isAuthorized(underlayName, credential, action, resourceType, resourceId)) {
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
      String underlayName,
      Object credential,
      Action action,
      ResourceType resourceType,
      @Nullable ResourceId resourceId) {
    UserId userId = getUserId(underlayName, credential);
    if (userId == null) {
      throw new BadRequestException("Invalid user id");
    }
    if (!resourceType.hasAction(action)) {
      throw new BadRequestException(
          "Action not available for resource type: " + action + ", " + resourceType);
    }

    return pluginService
        .getPlugin(underlayName, AccessControlPlugin.class)
        .isAuthorized(userId, action, resourceType, resourceId);
  }

  public UserId getUserId(String underlayName, Object credential) {
    return pluginService.getPlugin(underlayName, IdentityPlugin.class).getUserId(credential);
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
