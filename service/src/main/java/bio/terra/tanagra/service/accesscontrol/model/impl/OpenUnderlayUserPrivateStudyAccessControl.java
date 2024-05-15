package bio.terra.tanagra.service.accesscontrol.model.impl;

import bio.terra.tanagra.service.accesscontrol.AccessControlHelper;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol.model.StudyAccessControl;
import bio.terra.tanagra.service.authentication.UserId;
import java.util.List;

public class OpenUnderlayUserPrivateStudyAccessControl implements StudyAccessControl {
  private AccessControlHelper accessControlHelper;

  @Override
  public String getDescription() {
    return "Open underlay user private study-based access control";
  }

  @Override
  public void initialize(
      List<String> params,
      String basePath,
      String oauthClientId,
      AccessControlHelper accessControlHelper) {
    this.accessControlHelper = accessControlHelper;
  }

  @Override
  public ResourceCollection listUnderlays(UserId user, int offset, int limit) {
    // Everyone can see all underlays.
    return ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null);
  }

  @Override
  public Permissions getUnderlay(UserId user, ResourceId underlay) {
    // Everyone has all permissions on each underlay.
    return Permissions.allActions(ResourceType.UNDERLAY);
  }

  @Override
  public Permissions createStudy(UserId user) {
    // Everyone can create studies.
    return Permissions.forActions(ResourceType.STUDY, Action.CREATE);
  }

  @Override
  public ResourceCollection listStudies(UserId user, int offset, int limit) {
    return ResourceCollection.allResourcesAllPermissions(ResourceType.STUDY, null, user.getEmail());
  }

  @Override
  public Permissions getStudy(UserId user, ResourceId study) {
    return accessControlHelper.getStudyUser(study.getStudy()).equals(user.getEmail())
        ? Permissions.forActions(
            ResourceType.STUDY,
            Action.READ,
            Action.UPDATE,
            Action.DELETE,
            Action.CREATE_COHORT,
            Action.CREATE_CONCEPT_SET)
        : Permissions.empty(ResourceType.STUDY);
  }
}
