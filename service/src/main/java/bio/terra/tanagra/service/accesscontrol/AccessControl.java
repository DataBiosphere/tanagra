package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.service.accesscontrol.impl.AouWorkbenchAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.OpenAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.VerilyGroupsAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.VumcAdminAccessControl;
import bio.terra.tanagra.service.authentication.UserId;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/** Interface that all access control models must implement. */
public interface AccessControl {
  enum Model {
    OPEN_ACCESS(OpenAccessControl::new),
    VUMC_ADMIN(VumcAdminAccessControl::new),
    VERILY_GROUP(VerilyGroupsAccessControl::new),
    AOU_WORKBENCH(AouWorkbenchAccessControl::new);

    private final Supplier<AccessControl> createNewInstanceFn;

    Model(Supplier<AccessControl> createNewInstanceFn) {
      this.createNewInstanceFn = createNewInstanceFn;
    }

    public AccessControl createNewInstance() {
      return createNewInstanceFn.get();
    }
  }

  default void initialize(List<String> params, String baseUrl, String oauthClientId) {
    // Do nothing with parameters.
  }

  String getDescription();

  boolean isAuthorized(UserId user, Permissions permissions, @Nullable ResourceId resource);

  default ResourceCollection listAuthorizedResources(
      UserId user, Permissions permissions, int offset, int limit) {
    return listAllPermissions(user, permissions.getType(), null, offset, limit).filter(permissions);
  }

  default ResourceCollection listAuthorizedResources(
      UserId user,
      Permissions permissions,
      @Nullable ResourceId parentResource,
      int offset,
      int limit) {
    return listAllPermissions(user, permissions.getType(), parentResource, offset, limit)
        .filter(permissions);
  }

  default Permissions getPermissions(UserId user, ResourceId resource) {
    return listAllPermissions(user, resource.getType(), resource.getParent(), 0, Integer.MAX_VALUE)
        .getPermissions(resource);
  }

  ResourceCollection listAllPermissions(
      UserId user, ResourceType type, @Nullable ResourceId parentResource, int offset, int limit);
}
