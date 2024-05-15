package bio.terra.tanagra.service.accesscontrol;

import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ConceptSetService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;

public class AccessControlHelper {
  private final StudyService studyService;
  private final CohortService cohortService;
  private final ConceptSetService conceptSetService;
  private final ReviewService reviewService;

  public AccessControlHelper(
      StudyService studyService,
      CohortService cohortService,
      ConceptSetService conceptSetService,
      ReviewService reviewService) {
    this.studyService = studyService;
    this.cohortService = cohortService;
    this.conceptSetService = conceptSetService;
    this.reviewService = reviewService;
  }

  public String getStudyUser(String studyId) {
    return studyService.getStudy(studyId).getCreatedBy();
  }

  public String getCohortUser(String studyId, String cohortId) {
    return cohortService.getCohort(studyId, cohortId).getCreatedBy();
  }

  public String getConceptSetUser(String studyId, String conceptSetId) {
    return conceptSetService.getConceptSet(studyId, conceptSetId).getCreatedBy();
  }

  public String getReviewUser(String studyId, String cohortId, String reviewId) {
    return reviewService.getReview(studyId, cohortId, reviewId).getCreatedBy();
  }
}
