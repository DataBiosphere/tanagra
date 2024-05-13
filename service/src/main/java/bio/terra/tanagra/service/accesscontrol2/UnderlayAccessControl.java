package bio.terra.tanagra.service.accesscontrol2;

import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.authentication.UserId;
import java.util.Set;

public interface UnderlayAccessControl extends FineGrainedAccessControl {
  @Override
  default String getDescription() {
    return "Underlay-based access control";
  }

  @Override
  default ResourceCollection listStudies(UserId user, int offset, int limit) {
    return ResourceCollection.allResourcesAllPermissions(ResourceType.STUDY, null);
  }

  @Override
  default Permissions createStudy(UserId user) {
    return Permissions.forActions(ResourceType.STUDY, Set.of(Action.CREATE));
  }

  @Override
  default Permissions getStudy(UserId user, ResourceId study) {
    return Permissions.allActions(ResourceType.STUDY);
  }

  @Override
  default ResourceCollection listCohorts(UserId user, ResourceId study, int offset, int limit) {
    return ResourceCollection.allResourcesAllPermissions(ResourceType.COHORT, study);
  }

  @Override
  default Permissions getCohort(UserId user, ResourceId cohort) {
    return Permissions.allActions(ResourceType.COHORT);
  }

  @Override
  default ResourceCollection listDataFeatureSets(
      UserId user, ResourceId study, int offset, int limit) {
    return ResourceCollection.allResourcesAllPermissions(ResourceType.CONCEPT_SET, study);
  }

  @Override
  default Permissions getDataFeatureSet(UserId user, ResourceId dataFeatureSet) {
    return Permissions.allActions(ResourceType.CONCEPT_SET);
  }

  @Override
  default ResourceCollection listReviews(UserId user, ResourceId cohort, int offset, int limit) {
    return ResourceCollection.allResourcesAllPermissions(ResourceType.REVIEW, cohort);
  }

  @Override
  default Permissions getReview(UserId user, ResourceId review) {
    return Permissions.allActions(ResourceType.REVIEW);
  }

  @Override
  default ResourceCollection listAnnotations(
      UserId user, ResourceId cohort, int offset, int limit) {
    return ResourceCollection.allResourcesAllPermissions(ResourceType.ANNOTATION_KEY, cohort);
  }

  @Override
  default Permissions getAnnotation(UserId user, ResourceId annotation) {
    return Permissions.allActions(ResourceType.ANNOTATION_KEY);
  }

  @Override
  default Permissions getActivityLog(UserId user) {
    return Permissions.allActions(ResourceType.ACTIVITY_LOG);
  }
}
