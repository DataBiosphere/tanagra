package bio.terra.tanagra.app.controller;

import static bio.terra.tanagra.service.accesscontrol.Action.CREATE;
import static bio.terra.tanagra.service.accesscontrol.Action.DELETE;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_COUNTS;
import static bio.terra.tanagra.service.accesscontrol.Action.QUERY_INSTANCES;
import static bio.terra.tanagra.service.accesscontrol.Action.READ;
import static bio.terra.tanagra.service.accesscontrol.Action.UPDATE;
import static bio.terra.tanagra.service.accesscontrol.ResourceType.COHORT_REVIEW;

import bio.terra.tanagra.generated.controller.ReviewsV2Api;
import bio.terra.tanagra.generated.model.ApiAnnotationValueV2;
import bio.terra.tanagra.generated.model.ApiInstanceCountListV2;
import bio.terra.tanagra.generated.model.ApiReviewCountQueryV2;
import bio.terra.tanagra.generated.model.ApiReviewCreateInfoV2;
import bio.terra.tanagra.generated.model.ApiReviewInstanceListV2;
import bio.terra.tanagra.generated.model.ApiReviewInstanceV2;
import bio.terra.tanagra.generated.model.ApiReviewListV2;
import bio.terra.tanagra.generated.model.ApiReviewQueryV2;
import bio.terra.tanagra.generated.model.ApiReviewUpdateInfoV2;
import bio.terra.tanagra.generated.model.ApiReviewV2;
import bio.terra.tanagra.generated.model.ApiValueDisplayV2;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable.BinaryOperator;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
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
import bio.terra.tanagra.service.artifact.Annotation;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Review;
import bio.terra.tanagra.service.auth.UserId;
import bio.terra.tanagra.service.instances.AnnotationFilter;
import bio.terra.tanagra.service.instances.EntityInstance;
import bio.terra.tanagra.service.instances.EntityInstanceCount;
import bio.terra.tanagra.service.instances.EntityQueryRequest;
import bio.terra.tanagra.service.instances.ReviewInstance;
import bio.terra.tanagra.service.instances.ReviewQueryOrderBy;
import bio.terra.tanagra.service.instances.filter.AttributeFilter;
import bio.terra.tanagra.service.instances.filter.BooleanAndOrFilter;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.service.utils.ToApiConversionUtils;
import bio.terra.tanagra.underlay.Attribute;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
        UserId.currentUser(), CREATE, COHORT_REVIEW, new ResourceId(cohortId));

    // Generate a random 10-character alphanumeric string for the new review ID.
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
    accessControlService.throwIfUnauthorized(
        UserId.currentUser(), DELETE, COHORT_REVIEW, new ResourceId(reviewId));
    reviewService.deleteReview(studyId, cohortId, reviewId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<ApiReviewV2> getReview(String studyId, String cohortId, String reviewId) {
    accessControlService.throwIfUnauthorized(
        UserId.currentUser(), READ, COHORT_REVIEW, new ResourceId(reviewId));
    return ResponseEntity.ok(toApiObject(reviewService.getReview(studyId, cohortId, reviewId)));
  }

  @Override
  public ResponseEntity<ApiReviewInstanceListV2> listReviewInstancesAndAnnotations(
      String studyId, String cohortId, String reviewId, ApiReviewQueryV2 body) {
    accessControlService.throwIfUnauthorized(
        UserId.currentUser(), QUERY_INSTANCES, COHORT_REVIEW, new ResourceId(reviewId));

    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Entity entity = underlaysService.getUnderlay(cohort.getUnderlayName()).getPrimaryEntity();

    List<Attribute> attributes = new ArrayList<>();
    if (body.getIncludeAttributes() != null) {
      attributes =
          body.getIncludeAttributes().stream()
              .map(attrName -> querysService.getAttribute(entity, attrName))
              .collect(Collectors.toList());
    }

    // Make sure the entity ID attribute is included, so we can match the entity instances to their
    // associated annotations.
    if (!attributes.contains(entity.getIdAttribute())) {
      attributes.add(entity.getIdAttribute());
    }

    EntityFilter entityFilter =
        new AttributeFilter(
            entity.getIdAttribute(),
            FunctionFilterVariable.FunctionTemplate.IN,
            reviewService.getPrimaryEntityIds(studyId, cohortId, reviewId));
    if (body.getEntityFilter() != null) {
      entityFilter =
          new BooleanAndOrFilter(
              BooleanAndOrFilterVariable.LogicalOperator.AND,
              List.of(
                  entityFilter,
                  fromApiConversionService.fromApiObject(
                      body.getEntityFilter(), entity, cohort.getUnderlayName())));
    }

    QueryRequest queryRequest =
        querysService.buildInstancesQuery(
            new EntityQueryRequest.Builder()
                .entity(entity)
                .mappingType(Underlay.MappingType.INDEX)
                .selectAttributes(attributes)
                .selectHierarchyFields(Collections.EMPTY_LIST)
                .selectRelationshipFields(Collections.EMPTY_LIST)
                .filter(entityFilter)
                .build());
    DataPointer indexDataPointer =
        entity.getMapping(Underlay.MappingType.INDEX).getTablePointer().getDataPointer();
    List<EntityInstance> entityInstances =
        querysService.runInstancesQuery(
            indexDataPointer,
            attributes,
            Collections.EMPTY_LIST,
            Collections.EMPTY_LIST,
            queryRequest);

    AnnotationFilter annotationFilter;
    if (body.getAnnotationFilter() != null) {
      Annotation annotation =
          annotationService.getAnnotation(
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
    List<AnnotationValue> annotationValues =
        annotationService.getAnnotationValues(studyId, cohortId, reviewId);

    // Merge entity instances and annotation values.
    List<ReviewInstance> reviewInstances = new ArrayList<>();
    entityInstances.stream()
        .forEach(
            ei -> {
              Literal entityInstanceId =
                  ei.getAttributeValues().get(entity.getIdAttribute()).getValue();

              // TODO: Handle ID data types other than long.
              String entityInstanceIdStr = entityInstanceId.getInt64Val().toString();

              List<AnnotationValue> associatedAnnotationValues =
                  annotationValues.stream()
                      .filter(av -> av.getEntityInstanceId().equals(entityInstanceIdStr))
                      .collect(Collectors.toList());

              if (annotationFilter == null
                  || annotationFilter.isMatch(associatedAnnotationValues)) {
                reviewInstances.add(
                    new ReviewInstance(ei.getAttributeValues(), associatedAnnotationValues));
              }
            });

    List<ReviewQueryOrderBy> reviewOrderBys = new ArrayList<>();
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
                  reviewOrderBys.add(
                      new ReviewQueryOrderBy(
                          querysService.getAttribute(entity, attrName), direction));
                } else {
                  reviewOrderBys.add(
                      new ReviewQueryOrderBy(
                          annotationService.getAnnotation(
                              studyId, cohortId, orderBy.getAnnotation()),
                          direction));
                }
              });
    }

    // Order by the attributes and annotation values, preserving the order of operations of the
    // order by list.
    if (!reviewOrderBys.isEmpty()) {
      Comparator<ReviewInstance> comparator = null;
      for (ReviewQueryOrderBy reviewOrderBy : reviewOrderBys) {
        if (comparator == null) {
          comparator = Comparator.comparing(Function.identity(), reviewOrderBy::compare);
        } else {
          comparator = comparator.thenComparing(Function.identity(), reviewOrderBy::compare);
        }
      }
      reviewInstances.sort(comparator);
    }

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
        UserId.currentUser(), QUERY_COUNTS, COHORT_REVIEW, new ResourceId(reviewId));

    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Entity entity = underlaysService.getUnderlay(cohort.getUnderlayName()).getPrimaryEntity();

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
            reviewService.getPrimaryEntityIds(studyId, cohortId, reviewId));

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

  @Override
  public ResponseEntity<ApiReviewListV2> listReviews(
      String studyId, String cohortId, Integer offset, Integer limit) {
    ResourceIdCollection authorizedReviewIds =
        accessControlService.listResourceIds(UserId.currentUser(), COHORT_REVIEW, offset, limit);
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
    accessControlService.throwIfUnauthorized(
        UserId.currentUser(), UPDATE, COHORT_REVIEW, new ResourceId(reviewId));
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
}
