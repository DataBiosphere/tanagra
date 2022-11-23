package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT_REVIEW;

import bio.terra.tanagra.generated.controller.ReviewsV2Api;
import bio.terra.tanagra.generated.model.ApiReviewCreateInfoV2;
import bio.terra.tanagra.generated.model.ApiReviewInstanceListV2;
import bio.terra.tanagra.generated.model.ApiReviewListV2;
import bio.terra.tanagra.generated.model.ApiReviewQueryV2;
import bio.terra.tanagra.generated.model.ApiReviewUpdateInfoV2;
import bio.terra.tanagra.generated.model.ApiReviewV2;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.CohortService;
import bio.terra.tanagra.service.FromApiConversionService;
import bio.terra.tanagra.service.ReviewService;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Review;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import bio.terra.tanagra.underlay.Underlay;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ReviewsV2ApiController implements ReviewsV2Api {
  private final ReviewService reviewService;
  private final CohortService cohortService;
  private final UnderlaysService underlaysService;
  private final AccessControlService accessControlService;
  private final FromApiConversionService fromApiConversionService;

  @Autowired
  public ReviewsV2ApiController(
      ReviewService reviewService,
      CohortService cohortService,
      UnderlaysService underlaysService,
      AccessControlService accessControlService,
      FromApiConversionService fromApiConversionService) {
    this.reviewService = reviewService;
    this.cohortService = cohortService;
    this.underlaysService = underlaysService;
    this.accessControlService = accessControlService;
    this.fromApiConversionService = fromApiConversionService;
  }

  @Override
  public ResponseEntity<ApiReviewV2> createReview(
      String studyId, String cohortId, ApiReviewCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(null, CREATE, COHORT_REVIEW, new ResourceId(cohortId));

    // Generate a random 10-character alphanumeric strings for the new review ID.
    String newReviewId = RandomStringUtils.randomAlphanumeric(10);

    Review reviewToCreate =
        Review.builder()
            .reviewId(newReviewId)
            .displayName(body.getDisplayName())
            .description(body.getDescription())
            .size(body.getSize())
            .build();

    // TODO: Move this to the ReviewService once we can build the EntityFilter from the Cohort on
    // the backend, rather than having the UI pass it in.
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Underlay underlay = underlaysService.getUnderlay(cohort.getUnderlayName());
    EntityFilter entityFilter =
        fromApiConversionService.fromApiObject(
            body.getFilter(), underlay.getPrimaryEntity(), underlay.getName());

    reviewService.createReview(studyId, cohortId, reviewToCreate, entityFilter, underlay);
    return ResponseEntity.ok(toApiObject(reviewService.getReview(studyId, cohortId, newReviewId)));
  }

  @Override
  public ResponseEntity<Void> deleteReview(String studyId, String cohortId, String reviewId) {
    accessControlService.throwIfUnauthorized(null, DELETE, COHORT_REVIEW, new ResourceId(reviewId));
    reviewService.deleteReview(studyId, cohortId, reviewId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiReviewV2> getReview(String studyId, String cohortId, String reviewId) {
    accessControlService.throwIfUnauthorized(null, READ, COHORT_REVIEW, new ResourceId(reviewId));
    return ResponseEntity.ok(toApiObject(reviewService.getReview(studyId, cohortId, reviewId)));
  }

  @Override
  public ResponseEntity<ApiReviewInstanceListV2> listInstancesAndAnnotations(
      String studyId, String cohortId, String reviewId, ApiReviewQueryV2 body) {
    return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
  }

  @Override
  public ResponseEntity<ApiReviewListV2> listReviews(
      String studyId, String cohortId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedReviewIds =
        accessControlService.listResourceIds(COHORT_REVIEW, offset, limit);
    List<Review> authorizedReviews;
    if (authorizedReviewIds.isAllResourceIds()) {
      authorizedReviews = reviewService.getAllReviews(studyId, cohortId, offset, limit);
    } else {
      authorizedReviews =
          reviewService.getReviews(
              studyId,
              cohortId,
              authorizedReviewIds.getResourceIds().stream()
                  .map(ResourceId::getId)
                  .collect(Collectors.toList()),
              offset,
              limit);
    }

    ApiReviewListV2 apiReviews = new ApiReviewListV2();
    authorizedReviews.stream()
        .forEach(
            review -> {
              apiReviews.add(toApiObject(review));
            });
    return ResponseEntity.ok(apiReviews);
  }

  @Override
  public ResponseEntity<ApiReviewV2> updateReview(
      String studyId, String cohortId, String reviewId, ApiReviewUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(null, UPDATE, COHORT_REVIEW, new ResourceId(reviewId));
    Review updatedReview =
        reviewService.updateReview(
            studyId, cohortId, reviewId, body.getDisplayName(), body.getDescription());
    return ResponseEntity.ok(toApiObject(updatedReview));
  }

  private static ApiReviewV2 toApiObject(Review review) {
    return new ApiReviewV2()
        .id(review.getReviewId())
        .displayName(review.getDisplayName())
        .description(review.getDescription())
        .size(review.getSize())
        .created(review.getCreatedUTC())
        .cohort(ToApiConversionUtils.toApiObject(review.getCohort()));
  }
}
