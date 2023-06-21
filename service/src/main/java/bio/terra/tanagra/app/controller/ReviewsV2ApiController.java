package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT_REVIEW;

import bio.terra.tanagra.app.auth.SpringAuthentication;
import bio.terra.tanagra.generated.controller.ReviewsV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.CohortService;
import bio.terra.tanagra.service.FromApiConversionService;
import bio.terra.tanagra.service.ReviewService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Review;
import bio.terra.tanagra.service.instances.EntityCountResult;
import bio.terra.tanagra.service.instances.ReviewInstance;
import bio.terra.tanagra.service.instances.ReviewQueryResult;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.ValueDisplay;
import bio.terra.tanagra.utils.SqlFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ReviewsV2ApiController implements ReviewsV2Api {
  private final ReviewService reviewService;
  private final CohortService cohortService;
  private final AccessControlService accessControlService;
  private final FromApiConversionService fromApiConversionService;

  @Autowired
  public ReviewsV2ApiController(
      ReviewService reviewService,
      CohortService cohortService,
      AccessControlService accessControlService,
      FromApiConversionService fromApiConversionService) {
    this.reviewService = reviewService;
    this.cohortService = cohortService;
    this.accessControlService = accessControlService;
    this.fromApiConversionService = fromApiConversionService;
  }

  @Override
  public ResponseEntity<ApiReviewV2> createReview(
      String studyId, String cohortId, ApiReviewCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        CREATE,
        COHORT_REVIEW,
        ResourceId.forCohort(studyId, cohortId));

    // TODO: Remove the entity filter from here once we store it for the cohort.
    EntityFilter entityFilter =
        fromApiConversionService.fromApiObject(body.getFilter(), studyId, cohortId);

    Review createdReview =
        reviewService.createReview(
            studyId,
            cohortId,
            Review.builder()
                .displayName(body.getDisplayName())
                .description(body.getDescription())
                .size(body.getSize()),
            SpringAuthentication.getCurrentUser().getEmail(),
            entityFilter);
    return ResponseEntity.ok(
        toApiObject(createdReview, cohortService.getCohort(studyId, cohortId)));
  }

  @Override
  public ResponseEntity<Void> deleteReview(String studyId, String cohortId, String reviewId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        DELETE,
        COHORT_REVIEW,
        ResourceId.forReview(studyId, cohortId, reviewId));
    reviewService.deleteReview(studyId, cohortId, reviewId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiReviewV2> getReview(String studyId, String cohortId, String reviewId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        READ,
        COHORT_REVIEW,
        ResourceId.forReview(studyId, cohortId, reviewId));
    return ResponseEntity.ok(
        toApiObject(
            reviewService.getReview(studyId, cohortId, reviewId),
            cohortService.getCohort(studyId, cohortId)));
  }

  @Override
  public ResponseEntity<ApiReviewListV2> listReviews(
      String studyId, String cohortId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedReviewIds =
        accessControlService.listResourceIds(
            SpringAuthentication.getCurrentUser(),
            COHORT_REVIEW,
            ResourceId.forCohort(studyId, cohortId),
            offset,
            limit);
    ApiReviewListV2 apiReviews = new ApiReviewListV2();
    reviewService.listReviews(authorizedReviewIds, studyId, cohortId, offset, limit).stream()
        .forEach(
            review ->
                apiReviews.add(toApiObject(review, cohortService.getCohort(studyId, cohortId))));
    return ResponseEntity.ok(apiReviews);
  }

  @Override
  public ResponseEntity<ApiReviewV2> updateReview(
      String studyId, String cohortId, String reviewId, ApiReviewUpdateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        UPDATE,
        COHORT_REVIEW,
        ResourceId.forReview(studyId, cohortId, reviewId));
    Review updatedReview =
        reviewService.updateReview(
            studyId,
            cohortId,
            reviewId,
            SpringAuthentication.getCurrentUser().getEmail(),
            body.getDisplayName(),
            body.getDescription());
    return ResponseEntity.ok(
        toApiObject(updatedReview, cohortService.getCohort(studyId, cohortId)));
  }

  @Override
  public ResponseEntity<ApiReviewInstanceListResultV2> listReviewInstancesAndAnnotations(
      String studyId, String cohortId, String reviewId, ApiReviewQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        QUERY_INSTANCES,
        COHORT_REVIEW,
        ResourceId.forReview(studyId, cohortId, reviewId));
    ReviewQueryResult reviewQueryResult =
        reviewService.listReviewInstances(
            studyId,
            cohortId,
            reviewId,
            fromApiConversionService.fromApiObject(body, studyId, cohortId));
    return ResponseEntity.ok(
        new ApiReviewInstanceListResultV2()
            .instances(
                reviewQueryResult.getReviewInstances().stream()
                    .map(reviewInstance -> toApiObject(reviewInstance))
                    .collect(Collectors.toList()))
            .sql(SqlFormatter.format(reviewQueryResult.getSql()))
            .pageMarker(
                reviewQueryResult.getPageMarker() == null
                    ? null
                    : reviewQueryResult.getPageMarker().serialize()));
  }

  @Override
  public ResponseEntity<ApiInstanceCountListV2> countReviewInstances(
      String studyId, String cohortId, String reviewId, ApiReviewCountQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        QUERY_COUNTS,
        COHORT_REVIEW,
        ResourceId.forReview(studyId, cohortId, reviewId));
    EntityCountResult countResult =
        reviewService.countReviewInstances(studyId, cohortId, reviewId, body.getAttributes());
    ApiInstanceCountListV2 apiCounts =
        new ApiInstanceCountListV2().sql(SqlFormatter.format(countResult.getSql()));
    countResult.getEntityCounts().stream()
        .forEach(count -> apiCounts.addInstanceCountsItem(ToApiConversionUtils.toApiObject(count)));
    return ResponseEntity.ok(apiCounts);
  }

  private ApiReviewInstanceV2 toApiObject(ReviewInstance reviewInstance) {
    Map<String, ApiValueDisplayV2> attributes = new HashMap<>();
    for (Map.Entry<Attribute, ValueDisplay> attributeValue :
        reviewInstance.getAttributeValues().entrySet()) {
      attributes.put(
          attributeValue.getKey().getName(),
          ToApiConversionUtils.toApiObject(attributeValue.getValue()));
    }

    Map<String, List<ApiAnnotationValueV2>> annotationValues = new HashMap<>();
    for (AnnotationValue annotationValue : reviewInstance.getAnnotationValues()) {
      String annotationKeyId = annotationValue.getAnnotationKeyId();
      if (!annotationValues.containsKey(annotationKeyId)) {
        annotationValues.put(
            annotationKeyId,
            new ArrayList<>(Arrays.asList(ToApiConversionUtils.toApiObject(annotationValue))));
      } else {
        annotationValues
            .get(annotationKeyId)
            .add(ToApiConversionUtils.toApiObject(annotationValue));
      }
    }

    return new ApiReviewInstanceV2().attributes(attributes).annotations(annotationValues);
  }

  private static ApiReviewV2 toApiObject(Review review, Cohort cohort) {
    // TODO: Remove the cohort argument here once we handle cohort revisions in the API objects.
    return new ApiReviewV2()
        .id(review.getId())
        .displayName(review.getDisplayName())
        .description(review.getDescription())
        .size(review.getSize())
        .created(review.getCreated())
        .createdBy(review.getCreatedBy())
        .lastModified(review.getLastModified())
        .cohort(
            new ApiCohortV2()
                .id(review.getRevision().getId())
                .underlayName(cohort.getUnderlay())
                .displayName(cohort.getDisplayName())
                .description(cohort.getDescription())
                .created(cohort.getCreated())
                .createdBy(cohort.getCreatedBy())
                .lastModified(cohort.getLastModified())
                .criteriaGroupSections(
                    review.getRevision().getSections().stream()
                        .map(criteriaGroup -> ToApiConversionUtils.toApiObject(criteriaGroup))
                        .collect(Collectors.toList())));
  }
}
