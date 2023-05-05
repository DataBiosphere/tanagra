package bio.terra.tanagra.service;

import bio.terra.tanagra.app.configuration.FeatureConfiguration;
import bio.terra.tanagra.db.ReviewDao;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.query.filtervariable.FunctionFilterVariable;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.artifact.AnnotationValue;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Review;
import bio.terra.tanagra.service.instances.*;
import bio.terra.tanagra.service.instances.filter.AttributeFilter;
import bio.terra.tanagra.service.instances.filter.BooleanAndOrFilter;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.underlay.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReviewService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewService.class);

  private final CohortService cohortService;
  private final UnderlaysService underlaysService;

  private final AnnotationService annotationService;
  private final QuerysService querysService;
  private final ReviewDao reviewDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public ReviewService(
      CohortService cohortService,
      UnderlaysService underlaysService,
      AnnotationService annotationService,
      QuerysService querysService,
      ReviewDao reviewDao,
      FeatureConfiguration featureConfiguration) {
    this.cohortService = cohortService;
    this.underlaysService = underlaysService;
    this.annotationService = annotationService;
    this.querysService = querysService;
    this.reviewDao = reviewDao;
    this.featureConfiguration = featureConfiguration;
  }

  /** Create a review and a list of the primary entity instance ids it contains. */
  public Review createReview(
      String studyId,
      String cohortId,
      Review.Builder reviewBuilder,
      String userEmail,
      EntityFilter entityFilter) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Build a query of a random sample of primary entity instance ids in the cohort.
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Underlay underlay = underlaysService.getUnderlay(cohort.getUnderlay());
    TableVariable entityTableVar =
        TableVariable.forPrimary(
            underlay.getPrimaryEntity().getMapping(Underlay.MappingType.INDEX).getTablePointer());
    List<TableVariable> tableVars = Lists.newArrayList(entityTableVar);
    AttributeMapping idAttributeMapping =
        underlay.getPrimaryEntity().getIdAttribute().getMapping(Underlay.MappingType.INDEX);
    Query query =
        new Query.Builder()
            .select(idAttributeMapping.buildFieldVariables(entityTableVar, tableVars))
            .tables(tableVars)
            .where(entityFilter.getFilterVariable(entityTableVar, tableVars))
            .orderBy(List.of(OrderByVariable.forRandom()))
            .limit(reviewBuilder.getSize())
            .build();
    QueryRequest queryRequest =
        new QueryRequest(
            query.renderSQL(), new ColumnHeaderSchema(idAttributeMapping.buildColumnSchemas()));
    LOGGER.debug("RANDOM SAMPLE primary entity instance ids: {}", queryRequest.getSql());

    // Run the query and get an iterator to its results.
    DataPointer dataPointer =
        underlay
            .getPrimaryEntity()
            .getMapping(Underlay.MappingType.INDEX)
            .getTablePointer()
            .getDataPointer();
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    return createReviewHelper(studyId, cohortId, reviewBuilder, userEmail, queryResult);
  }

  @VisibleForTesting
  public Review createReviewHelper(
      String studyId,
      String cohortId,
      Review.Builder reviewBuilder,
      String userEmail,
      QueryResult queryResult) {
    featureConfiguration.artifactStorageEnabledCheck();
    if (!queryResult.getRowResults().iterator().hasNext()) {
      throw new IllegalArgumentException("Cannot create a review with an empty query result");
    }
    reviewDao.createReview(
        cohortId,
        reviewBuilder.createdBy(userEmail).lastModifiedBy(userEmail).build(),
        queryResult);
    return reviewDao.getReview(reviewBuilder.getId());
  }

  /** Delete a review and all the primary entity instance ids and annotation values it contains. */
  public void deleteReview(String studyId, String cohortId, String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    reviewDao.deleteReview(reviewId);
  }

  /** List reviews with their cohort revisions. */
  public List<Review> listReviews(
      ResourceIdCollection authorizedReviewIds,
      String studyId,
      String cohortId,
      int offset,
      int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    if (authorizedReviewIds.isAllResourceIds()) {
      return reviewDao.getAllReviews(cohortId, offset, limit);
    } else {
      return reviewDao.getReviewsMatchingList(
          authorizedReviewIds.getResourceIds().stream()
              .map(ResourceId::getId)
              .collect(Collectors.toSet()),
          offset,
          limit);
    }
  }

  /** Retrieve a review with its cohort revision. */
  public Review getReview(String studyId, String cohortId, String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return reviewDao.getReview(reviewId);
  }

  /** Update a review's metadata. */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Review updateReview(
      String studyId,
      String cohortId,
      String reviewId,
      String userEmail,
      @Nullable String displayName,
      @Nullable String description) {
    featureConfiguration.artifactStorageEnabledCheck();
    reviewDao.updateReview(reviewId, userEmail, displayName, description);
    return reviewDao.getReview(reviewId);
  }

  /**
   * Run a breakdown query on all the entity instances that are part of a review. Return the counts
   * and the generated SQL string.
   */
  public Pair<String, List<EntityInstanceCount>> countReviewInstances(
      String studyId, String cohortId, String reviewId, List<String> groupByAttributeNames) {
    Cohort cohort = cohortService.getCohort(studyId, cohortId);
    Entity entity = underlaysService.getUnderlay(cohort.getUnderlay()).getPrimaryEntity();
    List<Attribute> groupByAttributes =
        groupByAttributeNames.stream()
            .map(attrName -> entity.getAttribute(attrName))
            .collect(Collectors.toList());

    EntityFilter entityFilter =
        new AttributeFilter(
            entity.getIdAttribute(),
            FunctionFilterVariable.FunctionTemplate.IN,
            reviewDao.getPrimaryEntityIds(reviewId));
    QueryRequest queryRequest =
        querysService.buildInstanceCountsQuery(
            entity, Underlay.MappingType.INDEX, groupByAttributes, entityFilter);
    return Pair.of(
        queryRequest.getSql(),
        querysService.runInstanceCountsQuery(
            entity.getMapping(Underlay.MappingType.INDEX).getTablePointer().getDataPointer(),
            groupByAttributes,
            queryRequest));
  }

  public List<ReviewInstance> listReviewInstances(
      String studyId,
      String cohortId,
      String reviewId,
      ReviewQueryRequest.Builder reviewQueryRequestBuilder) {
    ReviewQueryRequest reviewQueryRequest =
        reviewQueryRequestBuilder
            .entityInstanceIds(reviewDao.getPrimaryEntityIds(reviewId))
            .annotationValues(annotationService.listAnnotationValues(studyId, cohortId, reviewId))
            .build();

    // Make sure the entity ID attribute is included, so we can match the entity instances to their
    // associated annotations.
    Attribute idAttribute = reviewQueryRequest.getEntity().getIdAttribute();
    if (!reviewQueryRequest.getAttributes().contains(idAttribute)) {
      reviewQueryRequest.addAttribute(idAttribute);
    }

    // Add a filter on the entity: ID is included in the review.
    EntityFilter entityFilter =
        new AttributeFilter(
            idAttribute,
            FunctionFilterVariable.FunctionTemplate.IN,
            reviewQueryRequest.getEntityInstanceIds());
    if (reviewQueryRequest.getEntityFilter() != null) {
      entityFilter =
          new BooleanAndOrFilter(
              BooleanAndOrFilterVariable.LogicalOperator.AND,
              List.of(entityFilter, reviewQueryRequest.getEntityFilter()));
    }

    // Build and run the query for entity instances against the index dataset.
    QueryRequest queryRequest =
        querysService.buildInstancesQuery(
            new EntityQueryRequest.Builder()
                .entity(reviewQueryRequest.getEntity())
                .mappingType(Underlay.MappingType.INDEX)
                .selectAttributes(reviewQueryRequest.getAttributes())
                .selectHierarchyFields(Collections.EMPTY_LIST)
                .selectRelationshipFields(Collections.EMPTY_LIST)
                .filter(entityFilter)
                .build());
    DataPointer indexDataPointer =
        reviewQueryRequest
            .getEntity()
            .getMapping(Underlay.MappingType.INDEX)
            .getTablePointer()
            .getDataPointer();
    List<EntityInstance> entityInstances =
        querysService.runInstancesQuery(
            indexDataPointer,
            reviewQueryRequest.getAttributes(),
            Collections.EMPTY_LIST,
            Collections.EMPTY_LIST,
            queryRequest);

    // Merge entity instances and annotation values, filtering out any instances that don't match
    // the annotation filter (if specified).
    List<ReviewInstance> reviewInstances = new ArrayList<>();
    entityInstances.stream()
        .forEach(
            ei -> {
              Literal entityInstanceId = ei.getAttributeValues().get(idAttribute).getValue();

              // TODO: Handle ID data types other than long.
              String entityInstanceIdStr = entityInstanceId.getInt64Val().toString();

              List<AnnotationValue> associatedAnnotationValues =
                  reviewQueryRequest.getAnnotationValues().stream()
                      .filter(av -> av.getInstanceId().equals(entityInstanceIdStr))
                      .collect(Collectors.toList());

              if (!reviewQueryRequest.hasAnnotationFilter()
                  || reviewQueryRequest.getAnnotationFilter().isMatch(associatedAnnotationValues)) {
                reviewInstances.add(
                    new ReviewInstance(ei.getAttributeValues(), associatedAnnotationValues));
              }
            });

    // Order by the attributes and annotation values, preserving the list order.
    if (!reviewQueryRequest.getOrderBys().isEmpty()) {
      Comparator<ReviewInstance> comparator = null;
      for (ReviewQueryOrderBy reviewOrderBy : reviewQueryRequest.getOrderBys()) {
        if (comparator == null) {
          comparator = Comparator.comparing(Function.identity(), reviewOrderBy::compare);
        } else {
          comparator = comparator.thenComparing(Function.identity(), reviewOrderBy::compare);
        }
      }
      reviewInstances.sort(comparator);
    }
    return reviewInstances;
  }
}
