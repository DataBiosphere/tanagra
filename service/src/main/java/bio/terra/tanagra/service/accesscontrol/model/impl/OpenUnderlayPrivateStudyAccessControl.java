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
import java.util.Set;
import java.util.stream.Collectors;

public class OpenUnderlayPrivateStudyAccessControl implements StudyAccessControl {
  private static final Permissions STUDY_PERMISSIONS_WITHOUT_CREATE =
      Permissions.forActions(
          ResourceType.STUDY,
          Action.READ,
          Action.UPDATE,
          Action.DELETE,
          Action.CREATE_COHORT,
          Action.CREATE_FEATURE_SET);
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
    return ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null);
  }

  @Override
  public Permissions getUnderlay(UserId user, ResourceId underlay) {
    return Permissions.allActions(ResourceType.UNDERLAY);
  }

  @Override
  public Permissions createStudy(UserId user) {
    return Permissions.forActions(ResourceType.STUDY, Action.CREATE);
  }

  @Override
  public ResourceCollection listStudies(UserId user, int offset, int limit) {
    Set<ResourceId> authorizedStudies =
        accessControlHelper.listStudiesForUser(user.getEmail()).stream()
            .map(ResourceId::forStudy)
            .collect(Collectors.toSet());
    return authorizedStudies.isEmpty()
        ? ResourceCollection.empty(ResourceType.STUDY, null)
        : ResourceCollection.resourcesSamePermissions(
            STUDY_PERMISSIONS_WITHOUT_CREATE, authorizedStudies);
  }

  @Override
  public Permissions getStudy(UserId user, ResourceId study) {
    return accessControlHelper.getStudyUser(study.getStudy()).equals(user.getEmail())
        ? STUDY_PERMISSIONS_WITHOUT_CREATE
        : Permissions.empty(ResourceType.STUDY);
  }
}
