package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.service.authentication.UserId;

public interface StudyAccessControl extends FineGrainedAccessControl {
  @Override
  default String getDescription() {
    return "Study-based access control";
  }

  @Override
  default ResourceCollection listCohorts(UserId user, ResourceId study, int offset, int limit) {
    return listStudyDescendantResources(user, study, ResourceType.COHORT);
  }

  @Override
  default Permissions getCohort(UserId user, ResourceId cohort) {
    return getStudyDescendantPermissions(user, cohort);
  }

  @Override
  default ResourceCollection listDataFeatureSets(
      UserId user, ResourceId study, int offset, int limit) {
    return listStudyDescendantResources(user, study, ResourceType.CONCEPT_SET);
  }

  @Override
  default Permissions getDataFeatureSet(UserId user, ResourceId dataFeatureSet) {
    return getStudyDescendantPermissions(user, dataFeatureSet);
  }

  @Override
  default ResourceCollection listReviews(UserId user, ResourceId cohort, int offset, int limit) {
    return listStudyDescendantResources(user, cohort, ResourceType.REVIEW);
  }

  @Override
  default Permissions getReview(UserId user, ResourceId review) {
    return getStudyDescendantPermissions(user, review);
  }

  @Override
  default ResourceCollection listAnnotations(
      UserId user, ResourceId cohort, int offset, int limit) {
    return listStudyDescendantResources(user, cohort, ResourceType.ANNOTATION_KEY);
  }

  @Override
  default Permissions getAnnotation(UserId user, ResourceId annotation) {
    return getStudyDescendantPermissions(user, annotation);
  }

  @Override
  default Permissions getActivityLog(UserId user) {
    return Permissions.empty(ResourceType.ACTIVITY_LOG);
  }

  private ResourceCollection listStudyDescendantResources(
      UserId user, ResourceId parent, ResourceType childResourceType) {
    Permissions studyPermissions = getStudy(user, parent.getStudyResourceId());
    if (hasStudyUpdatePermission(studyPermissions)) {
      return ResourceCollection.allResourcesAllPermissions(childResourceType, parent);
    } else if (hasStudyReadPermission(studyPermissions)) {
      return ResourceCollection.allResourcesSamePermissions(
          Permissions.forActions(childResourceType, Action.READ), parent);
    } else {
      return ResourceCollection.empty(childResourceType, parent);
    }
  }

  private Permissions getStudyDescendantPermissions(UserId user, ResourceId childResourceId) {
    Permissions studyPermissions = getStudy(user, childResourceId.getStudyResourceId());
    if (hasStudyUpdatePermission(studyPermissions)) {
      return Permissions.allActions(childResourceId.getType());
    } else if (hasStudyReadPermission(studyPermissions)) {
      return Permissions.forActions(childResourceId.getType(), Action.READ);
    } else {
      return Permissions.empty(childResourceId.getType());
    }
  }

  private static boolean hasStudyReadPermission(Permissions studyPermissions) {
    return studyPermissions.contains(Permissions.forActions(ResourceType.STUDY, Action.READ));
  }

  private static boolean hasStudyUpdatePermission(Permissions studyPermissions) {
    return studyPermissions.contains(Permissions.forActions(ResourceType.STUDY, Action.UPDATE));
  }
}
