package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.service.auth.UserId;
import java.util.List;

/** Interface that all access control models must implement. */
public interface AccessControl {
  enum Model {
    OPEN_ACCESS,
    VUMC_ADMIN,
    VERILY_GROUP
  }

  default void initialize(List<String> params, String baseUrl, String oauthClientId) {
    // Do nothing with parameters.
  }

  String getDescription();

  boolean isAuthorized(
      UserId userId, Action action, ResourceType resourceType, ResourceId resourceId);

  default ResourceIdCollection listResourceIds(
      UserId userId, ResourceType type, int offset, int limit) {
    return listResourceIds(userId, type, null, offset, limit);
  }

  ResourceIdCollection listResourceIds(
      UserId userId, ResourceType type, ResourceId parentResourceId, int offset, int limit);
}
