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
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.Review;
import bio.terra.tanagra.underlay.AttributeMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReviewService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewService.class);

  private final CohortService cohortService;
  private final UnderlaysService underlaysService;
  private final ReviewDao reviewDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public ReviewService(
      CohortService cohortService,
      UnderlaysService underlaysService,
      ReviewDao reviewDao,
      FeatureConfiguration featureConfiguration) {
    this.cohortService = cohortService;
    this.underlaysService = underlaysService;
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

  /** List the primary entity instance ids contained in a review. */
  public List<Literal> getPrimaryEntityIds(String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return reviewDao.getPrimaryEntityIds(reviewId);
  }
}
