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
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable.BinaryOperator;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.service.AccessControlService;
import bio.terra.tanagra.service.AnnotationService;
import bio.terra.tanagra.service.CohortService;
import bio.terra.tanagra.service.FromApiConversionService;
import bio.terra.tanagra.service.QuerysService;
import bio.terra.tanagra.service.ReviewService;
import bio.terra.tanagra.service.UnderlaysService;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.AnnotationValueV1;
import bio.terra.tanagra.service.instances.EntityInstanceCount;
import bio.terra.tanagra.service.instances.ReviewInstance;
import bio.terra.tanagra.service.instances.ReviewQueryOrderBy;
import bio.terra.tanagra.service.instances.ReviewQueryRequest;
import bio.terra.tanagra.service.instances.filter.AnnotationFilter;
import bio.terra.tanagra.service.instances.filter.AttributeFilter;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.service.model.AnnotationKey;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.Review;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import bio.terra.tanagra.service.utils.ValidationUtils;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.ValueDisplay;
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
  private final AnnotationService annotationService;
  private final UnderlaysService underlaysService;
  private final QuerysService querysService;
  private final AccessControlService accessControlService;
  private final FromApiConversionService fromApiConversionService;

  @Autowired
  public ReviewsV2ApiController(
      ReviewService reviewService,
      CohortService cohortService,
      AnnotationService annotationService,
      UnderlaysService underlaysService,
      QuerysService querysService,
      AccessControlService accessControlService,
      FromApiConversionService fromApiConversionService) {
    this.reviewService = reviewService;
    this.cohortService = cohortService;
    this.annotationService = annotationService;
    this.underlaysService = underlaysService;
    this.querysService = querysService;
    this.accessControlService = accessControlService;
    this.fromApiConversionService = fromApiConversionService;
  }

  @Override
  public ResponseEntity<ApiReviewV2> createReview(
      String studyId, String cohortId, ApiReviewCreateInfoV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), CREATE, COHORT_REVIEW, new ResourceId(cohortId));

    // TODO: Remove the entity filter from here once we store it for the cohort.
    ValidationUtils.validateApiFilter(body.getFilter());
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
        SpringAuthentication.getCurrentUser(), DELETE, COHORT_REVIEW, new ResourceId(reviewId));
    reviewService.deleteReview(studyId, cohortId, reviewId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiReviewV2> getReview(String studyId, String cohortId, String reviewId) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(), READ, COHORT_REVIEW, new ResourceId(reviewId));
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
            SpringAuthentication.getCurrentUser(), COHORT_REVIEW, offset, limit);
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
        SpringAuthentication.getCurrentUser(), UPDATE, COHORT_REVIEW, new ResourceId(reviewId));
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
  public ResponseEntity<ApiReviewInstanceListV2> listReviewInstancesAndAnnotations(
      String studyId, String cohortId, String reviewId, ApiReviewQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        QUERY_INSTANCES,
        COHORT_REVIEW,
        new ResourceId(reviewId));

    ValidationUtils.validateApiFilter(body.getEntityFilter());

    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Entity entity = underlaysService.getUnderlay(cohort.getUnderlay()).getPrimaryEntity();
    List<Attribute> attributes = new ArrayList<>();
    if (body.getIncludeAttributes() != null) {
      attributes =
          body.getIncludeAttributes().stream()
              .map(attrName -> querysService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }

    EntityFilter entityFilter =
        (body.getEntityFilter() != null)
            ? fromApiConversionService.fromApiObject(
                body.getEntityFilter(), entity, cohort.getUnderlay())
            : null;
    AnnotationFilter annotationFilter;
    if (body.getAnnotationFilter() != null) {
      AnnotationKey annotation =
          annotationService.getAnnotationKey(
              studyId, cohortId, body.getAnnotationFilter().getAnnotation());
      BinaryOperator operator =
          BinaryFilterVariable.BinaryOperator.valueOf(
              body.getAnnotationFilter().getOperator().name());
      annotationFilter =
          new AnnotationFilter(
              annotation,
              operator,
              FromApiConversionService.fromApiObject(body.getAnnotationFilter().getValue()));
    } else {
      annotationFilter = null;
    }

    List<ReviewQueryOrderBy> orderBys = new ArrayList<>();
    if (body.getOrderBys() != null) {
      body.getOrderBys().stream()
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
                          querysService.getAttribute(entity, attrName), direction));
                } else {
                  orderBys.add(
                      new ReviewQueryOrderBy(
                          annotationService.getAnnotationKey(
                              studyId, cohortId, orderBy.getAnnotation()),
                          direction));
                }
              });
    }

    List<ReviewInstance> reviewInstances =
        querysService.buildAndRunReviewInstancesQuery(
            new ReviewQueryRequest.Builder()
                .entity(entity)
                .mappingType(Underlay.MappingType.INDEX)
                .attributes(attributes)
                .entityFilter(entityFilter)
                .annotationFilter(annotationFilter)
                .entityInstanceIds(reviewService.getPrimaryEntityIds(reviewId))
                .annotationValues(annotationService.getAnnotationValues(reviewId))
                .orderBys(orderBys)
                .build());

    ApiReviewInstanceListV2 apiReviewInstances = new ApiReviewInstanceListV2();
    reviewInstances.stream()
        .forEach(
            reviewInstance -> {
              apiReviewInstances.add(toApiObject(reviewInstance));
            });
    return ResponseEntity.ok(apiReviewInstances);
  }

  @Override
  public ResponseEntity<ApiInstanceCountListV2> countReviewInstances(
      String studyId, String cohortId, String reviewId, ApiReviewCountQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        SpringAuthentication.getCurrentUser(),
        QUERY_COUNTS,
        COHORT_REVIEW,
        new ResourceId(reviewId));

    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Entity entity = underlaysService.getUnderlay(cohort.getUnderlay()).getPrimaryEntity();

    List<Attribute> attributes = new ArrayList<>();
    if (body.getAttributes() != null) {
      attributes =
          body.getAttributes().stream()
              .map(attrName -> querysService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }

    EntityFilter entityFilter =
        new AttributeFilter(
            entity.getIdAttribute(),
            FunctionFilterVariable.FunctionTemplate.IN,
            reviewService.getPrimaryEntityIds(reviewId));

    QueryRequest queryRequest =
        querysService.buildInstanceCountsQuery(
            entity, Underlay.MappingType.INDEX, attributes, entityFilter);
    List<EntityInstanceCount> entityInstanceCounts =
        querysService.runInstanceCountsQuery(
            entity.getMapping(Underlay.MappingType.INDEX).getTablePointer().getDataPointer(),
            attributes,
            queryRequest);

    return ResponseEntity.ok(
        new ApiInstanceCountListV2()
            .instanceCounts(
                entityInstanceCounts.stream()
                    .map(
                        entityInstanceCount ->
                            ToApiConversionUtils.toApiObject(entityInstanceCount))
                    .collect(Collectors.toList()))
            .sql(queryRequest.getSql()));
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
    for (AnnotationValueV1 annotationValue : reviewInstance.getAnnotationValues()) {
      String annotationId = annotationValue.getAnnotationId();
      if (!annotationValues.containsKey(annotationId)) {
        annotationValues.put(
            annotationId,
            new ArrayList<>(Arrays.asList(ToApiConversionUtils.toApiObject(annotationValue))));
      } else {
        annotationValues.get(annotationId).add(ToApiConversionUtils.toApiObject(annotationValue));
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
