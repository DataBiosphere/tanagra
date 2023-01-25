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
import bio.terra.tanagra.service.artifact.Review;
import bio.terra.tanagra.service.instances.filter.EntityFilter;
import bio.terra.tanagra.underlay.AttributeMapping;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Underlay;
import com.google.common.collect.Lists;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReviewService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewService.class);

  private final ReviewDao reviewDao;
  private final FeatureConfiguration featureConfiguration;

  @Autowired
  public ReviewService(ReviewDao reviewDao, FeatureConfiguration featureConfiguration) {
    this.reviewDao = reviewDao;
    this.featureConfiguration = featureConfiguration;
  }

  /** Create a new review. */
  public void createReview(
      String studyId,
      String cohortRevisionGroupId,
      Review review,
      EntityFilter entityFilter,
      Underlay underlay) {
    featureConfiguration.artifactStorageEnabledCheck();

    // Build a query of a random sample of primary entity instance ids in the cohort.
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
            .limit(review.getSize())
            .build();
    LOGGER.info("Generated query: {}", query.renderSQL());
    QueryRequest queryRequest =
        new QueryRequest(
            query.renderSQL(), new ColumnHeaderSchema(idAttributeMapping.buildColumnSchemas()));

    // Run the query and get an iterator to its results.
    DataPointer dataPointer =
        underlay
            .getPrimaryEntity()
            .getMapping(Underlay.MappingType.INDEX)
            .getTablePointer()
            .getDataPointer();
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    reviewDao.createReview(studyId, cohortRevisionGroupId, review, queryResult);
  }

  /** Delete an existing review. */
  public void deleteReview(String studyId, String cohortRevisionGroupId, String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    reviewDao.deleteReview(studyId, cohortRevisionGroupId, reviewId);
  }

  /** Retrieves a list of all reviews for a cohort. */
  public List<Review> getAllReviews(
      String studyId, String cohortRevisionGroupId, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return reviewDao.getAllReviews(studyId, cohortRevisionGroupId, offset, limit);
  }

  /** Retrieves a list of reviews by ID. */
  public List<Review> getReviews(
      String studyId, String cohortRevisionGroupId, List<String> reviewIds, int offset, int limit) {
    featureConfiguration.artifactStorageEnabledCheck();
    return reviewDao.getReviewsMatchingList(
        studyId, cohortRevisionGroupId, new HashSet<>(reviewIds), offset, limit);
  }

  /** Retrieves a review by ID. */
  public Review getReview(String studyId, String cohortRevisionGroupId, String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return reviewDao.getReview(studyId, cohortRevisionGroupId, reviewId);
  }

  /** Update an existing review. Currently, can change the review's display name or description. */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public Review updateReview(
      String studyId,
      String cohortRevisionGroupId,
      String reviewId,
      @Nullable String displayName,
      @Nullable String description) {
    featureConfiguration.artifactStorageEnabledCheck();
    reviewDao.updateReview(studyId, cohortRevisionGroupId, reviewId, displayName, description);
    return reviewDao.getReview(studyId, cohortRevisionGroupId, reviewId);
  }

  /** Retrieves a list of the frozen primary entity instance ids for this review. */
  public List<Literal> getPrimaryEntityIds(String reviewId) {
    featureConfiguration.artifactStorageEnabledCheck();
    return reviewDao.getPrimaryEntityIds(reviewId);
  }
}
