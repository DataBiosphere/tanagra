package bio.terra.tanagra.service.accesscontrol2;

import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.authentication.UserId;
import java.util.List;

public interface FineGrainedAccessControl {
  default String getDescription() {
    return "Fine-grained access control";
  }

  default void initialize(List<String> params, String basePath, String oauthClientId) {
    // Do nothing with parameters.
  }

  ResourceCollection listUnderlays(UserId user, int offset, int limit);

  Permissions getUnderlay(UserId user, ResourceId underlay);

  Permissions createStudy(UserId user);

  ResourceCollection listStudies(UserId user, int offset, int limit);

  Permissions getStudy(UserId user, ResourceId study);

  ResourceCollection listCohorts(UserId user, ResourceId study, int offset, int limit);

  Permissions getCohort(UserId user, ResourceId cohort);

  ResourceCollection listDataFeatureSets(UserId user, ResourceId study, int offset, int limit);

  Permissions getDataFeatureSet(UserId user, ResourceId dataFeatureSet);

  ResourceCollection listReviews(UserId user, ResourceId cohort, int offset, int limit);

  Permissions getReview(UserId user, ResourceId review);

  ResourceCollection listAnnotations(UserId user, ResourceId cohort, int offset, int limit);

  Permissions getAnnotation(UserId user, ResourceId annotation);

  Permissions getActivityLog(UserId user);
}
