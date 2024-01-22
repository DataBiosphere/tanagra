package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE_REVIEW;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.REVIEW;

import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.PageMarker;
import bio.terra.tanagra.api.query.count.CountQueryResult;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.api.shared.ValueDisplay;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.ReviewsApi;
import bio.terra.tanagra.generated.model.ApiAnnotationValue;
import bio.terra.tanagra.generated.model.ApiCohort;
import bio.terra.tanagra.generated.model.ApiInstanceCountList;
import bio.terra.tanagra.generated.model.ApiReview;
import bio.terra.tanagra.generated.model.ApiReviewCountQuery;
import bio.terra.tanagra.generated.model.ApiReviewCreateInfo;
import bio.terra.tanagra.generated.model.ApiReviewInstance;
import bio.terra.tanagra.generated.model.ApiReviewInstanceListResult;
import bio.terra.tanagra.generated.model.ApiReviewList;
import bio.terra.tanagra.generated.model.ApiReviewQuery;
import bio.terra.tanagra.generated.model.ApiReviewUpdateInfo;
import bio.terra.tanagra.generated.model.ApiValueDisplay;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.artifact.AnnotationService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.AnnotationValue;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.reviewquery.AnnotationFilter;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewInstance;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryOrderBy;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryRequest;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryResult;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
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
public class ReviewsApiController implements ReviewsApi {
  private final UnderlayService underlayService;
  private final ReviewService reviewService;
  private final CohortService cohortService;
  private final AnnotationService annotationService;

  private final AccessControlService accessControlService;

  @Autowired
  public ReviewsApiController(
      UnderlayService underlayService,
      ReviewService reviewService,
      CohortService cohortService,
      AnnotationService annotationService,
      AccessControlService accessControlService) {
    this.underlayService = underlayService;
    this.reviewService = reviewService;
    this.cohortService = cohortService;
    this.annotationService = annotationService;
    this.accessControlService = accessControlService;
  }

  @Override
  public ResponseEntity<ApiReview> createReview(
      String studyId, String cohortId, ApiReviewCreateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(COHORT, CREATE_REVIEW),
        ResourceId.forCohort(studyId, cohortId));

    // TODO: Remove the entity filter from here once we store it for the cohort.
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    EntityFilter entityFilter =
        FromApiUtils.fromApiObject(
            body.getFilter(), underlayService.getUnderlay(cohort.getUnderlay()));

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
        Permissions.forActions(REVIEW, DELETE),
        ResourceId.forReview(studyId, cohortId, reviewId));
    reviewService.deleteReview(
        studyId, cohortId, reviewId, SpringAuthentication.getCurrentUser().getEmail());
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiReview> getReview(String studyId, String cohortId, String reviewId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(REVIEW, READ),
        ResourceId.forReview(studyId, cohortId, reviewId));
    return ResponseEntity.ok(
        toApiObject(
            reviewService.getReview(studyId, cohortId, reviewId),
            cohortService.getCohort(studyId, cohortId)));
  }

  @Override
  public ResponseEntity<ApiReviewList> listReviews(
      String studyId, String cohortId, Integer offset, Integer limit) {
    ResourceCollection authorizedReviewIds =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(REVIEW, READ),
            ResourceId.forCohort(studyId, cohortId),
            offset,
            limit);
    ApiReviewList apiReviews = new ApiReviewList();
    reviewService.listReviews(authorizedReviewIds, offset, limit).stream()
        .forEach(
            review ->
                apiReviews.add(toApiObject(review, cohortService.getCohort(studyId, cohortId))));
    return ResponseEntity.ok(apiReviews);
  }

  @Override
  public ResponseEntity<ApiReview> updateReview(
      String studyId, String cohortId, String reviewId, ApiReviewUpdateInfo body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(REVIEW, UPDATE),
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
  public ResponseEntity<ApiReviewInstanceListResult> listReviewInstancesAndAnnotations(
      String studyId, String cohortId, String reviewId, ApiReviewQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(REVIEW, QUERY_INSTANCES),
        ResourceId.forReview(studyId, cohortId, reviewId));
    ReviewQueryResult reviewQueryResult =
        reviewService.listReviewInstances(
            studyId, cohortId, reviewId, fromApiObject(body, studyId, cohortId));
    return ResponseEntity.ok(
        new ApiReviewInstanceListResult()
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
  public ResponseEntity<ApiInstanceCountList> countReviewInstances(
      String studyId, String cohortId, String reviewId, ApiReviewCountQuery body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(REVIEW, QUERY_COUNTS),
        ResourceId.forReview(studyId, cohortId, reviewId));
    CountQueryResult countResult =
        reviewService.countReviewInstances(studyId, cohortId, reviewId, body.getAttributes());
    ApiInstanceCountList apiCounts =
        new ApiInstanceCountList().sql(SqlFormatter.format(countResult.getSql()));
    countResult.getCountInstances().stream()
        .forEach(count -> apiCounts.addInstanceCountsItem(ToApiUtils.toApiObject(count)));
    return ResponseEntity.ok(apiCounts);
  }

  public ReviewQueryRequest fromApiObject(ApiReviewQuery apiObj, String studyId, String cohortId) {
    FromApiUtils.validateApiFilter(apiObj.getEntityFilter());

    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Underlay underlay = underlayService.getUnderlay(cohort.getUnderlay());
    Entity entity = underlay.getPrimaryEntity();
    List<Attribute> attributes = new ArrayList<>();
    if (apiObj.getIncludeAttributes() != null) {
      attributes =
          apiObj.getIncludeAttributes().stream()
              .map(attrName -> entity.getAttribute(attrName))
              .collect(Collectors.toList());
    }

    EntityFilter entityFilter =
        (apiObj.getEntityFilter() != null)
            ? FromApiUtils.fromApiObject(apiObj.getEntityFilter(), underlay)
            : null;
    AnnotationFilter annotationFilter;
    if (apiObj.getAnnotationFilter() != null) {
      AnnotationKey annotation =
          annotationService.getAnnotationKey(
              studyId, cohortId, apiObj.getAnnotationFilter().getAnnotation());
      BinaryOperator operator =
          BinaryOperator.valueOf(apiObj.getAnnotationFilter().getOperator().name());
      annotationFilter =
          new AnnotationFilter(
              annotation,
              operator,
              FromApiUtils.fromApiObject(apiObj.getAnnotationFilter().getValue()));
    } else {
      annotationFilter = null;
    }

    List<ReviewQueryOrderBy> orderBys = new ArrayList<>();
    if (apiObj.getOrderBys() != null) {
      apiObj.getOrderBys().stream()
          .forEach(
              orderBy -> {
                OrderByDirection direction =
                    orderBy.getDirection() == null
                        ? OrderByDirection.ASCENDING
                        : OrderByDirection.valueOf(orderBy.getDirection().name());
                String attrName = orderBy.getAttribute();
                if (attrName != null) {
                  orderBys.add(new ReviewQueryOrderBy(entity.getAttribute(attrName), direction));
                } else {
                  orderBys.add(
                      new ReviewQueryOrderBy(
                          annotationService.getAnnotationKey(
                              studyId, cohortId, orderBy.getAnnotation()),
                          direction));
                }
              });
    }
    return ReviewQueryRequest.builder()
        .attributes(attributes)
        .entityFilter(entityFilter)
        .annotationFilter(annotationFilter)
        .orderBys(orderBys)
        .pageSize(apiObj.getPageSize())
        .pageMarker(PageMarker.deserialize(apiObj.getPageMarker()))
        .build();
  }

  private ApiReviewInstance toApiObject(ReviewInstance reviewInstance) {
    Map<String, ApiValueDisplay> attributes = new HashMap<>();
    for (Map.Entry<Attribute, ValueDisplay> attributeValue :
        reviewInstance.getAttributeValues().entrySet()) {
      attributes.put(
          attributeValue.getKey().getName(), ToApiUtils.toApiObject(attributeValue.getValue()));
    }

    Map<String, List<ApiAnnotationValue>> annotationValues = new HashMap<>();
    for (AnnotationValue annotationValue : reviewInstance.getAnnotationValues()) {
      String annotationKeyId = annotationValue.getAnnotationKeyId();
      if (!annotationValues.containsKey(annotationKeyId)) {
        annotationValues.put(
            annotationKeyId,
            new ArrayList<>(Arrays.asList(ToApiUtils.toApiObject(annotationValue))));
      } else {
        annotationValues.get(annotationKeyId).add(ToApiUtils.toApiObject(annotationValue));
      }
    }

    return new ApiReviewInstance()
        .stableIndex(reviewInstance.getStableIndex())
        .attributes(attributes)
        .annotations(annotationValues);
  }

  private static ApiReview toApiObject(Review review, Cohort cohort) {
    // TODO: Remove the cohort argument here once we handle cohort revisions in the API objects.
    return new ApiReview()
        .id(review.getId())
        .displayName(review.getDisplayName())
        .description(review.getDescription())
        .size(review.getSize())
        .created(review.getCreated())
        .createdBy(review.getCreatedBy())
        .lastModified(review.getLastModified())
        .cohort(
            new ApiCohort()
                .id(cohort.getId())
                .revisionId(review.getRevision().getId())
                .underlayName(cohort.getUnderlay())
                .displayName(cohort.getDisplayName())
                .description(cohort.getDescription())
                .created(cohort.getCreated())
                .createdBy(cohort.getCreatedBy())
                .lastModified(cohort.getLastModified())
                .criteriaGroupSections(
                    review.getRevision().getSections().stream()
                        .map(criteriaGroup -> ToApiUtils.toApiObject(criteriaGroup))
                        .collect(Collectors.toList())));
  }
}
