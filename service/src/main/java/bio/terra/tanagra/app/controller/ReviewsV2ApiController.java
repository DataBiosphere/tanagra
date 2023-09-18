package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.*;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.REVIEW;

import bio.terra.tanagra.api.query.EntityCountResult;
import bio.terra.tanagra.api.query.filter.EntityFilter;
import bio.terra.tanagra.app.authentication.SpringAuthentication;
import bio.terra.tanagra.app.controller.objmapping.FromApiUtils;
import bio.terra.tanagra.app.controller.objmapping.ToApiUtils;
import bio.terra.tanagra.generated.controller.ReviewsV2Api;
import bio.terra.tanagra.generated.model.*;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.PageMarker;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
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
import bio.terra.tanagra.service.query.*;
import bio.terra.tanagra.service.query.filter.AnnotationFilter;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.ValueDisplay;
import bio.terra.tanagra.utils.SqlFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ReviewsV2ApiController implements ReviewsV2Api {
  private final UnderlayService underlayService;
  private final ReviewService reviewService;
  private final CohortService cohortService;
  private final AnnotationService annotationService;

  private final AccessControlService accessControlService;

  @Autowired
  public ReviewsV2ApiController(
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
  public ResponseEntity<ApiReviewV2> createReview(
      String studyId, String cohortId, ApiReviewCreateInfoV2 body) {
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
  public ResponseEntity<ApiReviewV2> getReview(String studyId, String cohortId, String reviewId) {
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
  public ResponseEntity<ApiReviewListV2> listReviews(
      String studyId, String cohortId, Integer offset, Integer limit) {
    ResourceCollection authorizedReviewIds =
        accessControlService.listAuthorizedResources(
            SpringAuthentication.getCurrentUser(),
            Permissions.forActions(REVIEW, READ),
            ResourceId.forCohort(studyId, cohortId),
            offset,
            limit);
    ApiReviewListV2 apiReviews = new ApiReviewListV2();
    reviewService.listReviews(authorizedReviewIds, offset, limit).stream()
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
  public ResponseEntity<ApiReviewInstanceListResultV2> listReviewInstancesAndAnnotations(
      String studyId, String cohortId, String reviewId, ApiReviewQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        Permissions.forActions(REVIEW, QUERY_INSTANCES),
        ResourceId.forReview(studyId, cohortId, reviewId));
    ReviewQueryResult reviewQueryResult =
        reviewService.listReviewInstances(
            studyId, cohortId, reviewId, fromApiObject(body, studyId, cohortId));
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
        Permissions.forActions(REVIEW, QUERY_COUNTS),
        ResourceId.forReview(studyId, cohortId, reviewId));
    EntityCountResult countResult =
        reviewService.countReviewInstances(studyId, cohortId, reviewId, body.getAttributes());
    ApiInstanceCountListV2 apiCounts =
        new ApiInstanceCountListV2().sql(SqlFormatter.format(countResult.getSql()));
    countResult.getEntityCounts().stream()
        .forEach(count -> apiCounts.addInstanceCountsItem(ToApiUtils.toApiObject(count)));
    return ResponseEntity.ok(apiCounts);
  }

  public ReviewQueryRequest fromApiObject(
      ApiReviewQueryV2 apiObj, String studyId, String cohortId) {
    FromApiUtils.validateApiFilter(apiObj.getEntityFilter());

    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Entity entity = underlayService.getUnderlay(cohort.getUnderlay()).getPrimaryEntity();
    List<Attribute> attributes = new ArrayList<>();
    if (apiObj.getIncludeAttributes() != null) {
      attributes =
          apiObj.getIncludeAttributes().stream()
              .map(attrName -> FromApiUtils.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }

    EntityFilter entityFilter =
        (apiObj.getEntityFilter() != null)
            ? FromApiUtils.fromApiObject(apiObj.getEntityFilter(), entity, cohort.getUnderlay())
            : null;
    AnnotationFilter annotationFilter;
    if (apiObj.getAnnotationFilter() != null) {
      AnnotationKey annotation =
          annotationService.getAnnotationKey(
              studyId, cohortId, apiObj.getAnnotationFilter().getAnnotation());
      BinaryFilterVariable.BinaryOperator operator =
          BinaryFilterVariable.BinaryOperator.valueOf(
              apiObj.getAnnotationFilter().getOperator().name());
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
                  orderBys.add(
                      new ReviewQueryOrderBy(
                          FromApiUtils.getAttribute(entity, attrName), direction));
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

  private ApiReviewInstanceV2 toApiObject(ReviewInstance reviewInstance) {
    Map<String, ApiValueDisplayV2> attributes = new HashMap<>();
    for (Map.Entry<Attribute, ValueDisplay> attributeValue :
        reviewInstance.getAttributeValues().entrySet()) {
      attributes.put(
          attributeValue.getKey().getName(), ToApiUtils.toApiObject(attributeValue.getValue()));
    }

    Map<String, List<ApiAnnotationValueV2>> annotationValues = new HashMap<>();
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

    return new ApiReviewInstanceV2()
        .stableIndex(reviewInstance.getStableIndex())
        .attributes(attributes)
        .annotations(annotationValues);
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
                        .map(criteriaGroup -> ToApiUtils.toApiObject(criteriaGroup))
                        .collect(Collectors.toList())));
  }
}
