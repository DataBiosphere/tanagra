package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.db.exception.DuplicateStudyException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Review;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReviewDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewDao.class);

  // SQL query and row mapper for reading a review.
  private static final String REVIEW_SELECT_SQL =
      "SELECT r.cohort_id, r.review_id, r.display_name, r.description, r.size, r.created, r.created_by, r.last_modified FROM review AS r "
          + "JOIN cohort AS c ON c.cohort_id = r.cohort_id";
  private static final RowMapper<Review.Builder> REVIEW_ROW_MAPPER =
      (rs, rowNum) ->
          Review.builder()
              .cohortId(rs.getString("cohort_id"))
              .reviewId(rs.getString("review_id"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .size(rs.getInt("size"))
              .created(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
              .createdBy(rs.getString("created_by"))
              .lastModified(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")));

  // SQL query and row mapper for reading a review instance.
  private static final String REVIEW_INSTANCE_SELECT_SQL =
      "SELECT entity_instance_id FROM review_instance";
  private static final RowMapper<Literal> REVIEW_INSTANCE_ROW_MAPPER =
      (rs, rowNum) ->
          new Literal.Builder()
              // TODO: Support id data types other than long
              .int64Val(rs.getLong("entity_instance_id"))
              .dataType(Literal.DataType.INT64)
              .build();

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final CohortDao cohortDao;

  @Autowired
  public ReviewDao(NamedParameterJdbcTemplate jdbcTemplate, CohortDao cohortDao) {
    this.jdbcTemplate = jdbcTemplate;
    this.cohortDao = cohortDao;
  }

  /**
   * Create a new review. Freeze the cohort and save the sampled primary entity ids for this review.
   */
  @WriteTransaction
  public void createReview(
      String studyId, String cohortRevisionGroupId, Review review, QueryResult queryResult) {
    Cohort cohort = cohortDao.getCohortLatestVersionOrThrow(studyId, cohortRevisionGroupId);
    cohortDao.freezeCohortLatestVersionOrThrow(studyId, cohort.getCohortRevisionGroupId());

    final String sql =
        "INSERT INTO review (cohort_id, review_id, display_name, description, size, created, created_by) "
            + "VALUES (:cohort_id, :review_id, :display_name, :description, :size, :created, :created_by)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohort.getCohortId())
            .addValue("review_id", review.getReviewId())
            .addValue("display_name", review.getDisplayName())
            .addValue("description", review.getDescription())
            .addValue("size", review.getSize())
            // Don't need to set created. Liquibase defaultValueComputed handles that.
            .addValue("created_by", review.getCreatedBy());
    try {
      jdbcTemplate.update(sql, params);
      LOGGER.info("Inserted record for review {}", review.getReviewId());
    } catch (DuplicateKeyException dkEx) {
      if (dkEx.getMessage()
          .contains("duplicate key value violates unique constraint \"review_pkey\"")) {
        throw new DuplicateStudyException(
            String.format(
                "Review with id %s already exists - display name %s",
                review.getReviewId(), review.getDisplayName()),
            dkEx);
      } else {
        throw dkEx;
      }
    }

    final String reviewInstanceSql =
        "INSERT INTO review_instance (review_id, entity_instance_id) VALUES (:review_id, :entity_instance_id)";
    List<MapSqlParameterSource> paramSets = new ArrayList<>();
    Iterator<RowResult> rowResultsItr = queryResult.getRowResults().iterator();
    while (rowResultsItr.hasNext()) {
      paramSets.add(
          new MapSqlParameterSource()
              .addValue("review_id", review.getReviewId())
              // TODO: Support id data types other than long (e.g. by always converting them to
              // string).
              .addValue(
                  "entity_instance_id", rowResultsItr.next().get("id").getLong().getAsLong()));
    }
    jdbcTemplate.batchUpdate(reviewInstanceSql, paramSets.toArray(new MapSqlParameterSource[0]));
    LOGGER.info("Inserted primary entity instance records for review {}", review.getReviewId());
  }

  /** Delete a review. */
  @WriteTransaction
  public boolean deleteReview(String studyId, String cohortRevisionGroupId, String reviewId) {
    final String sql = "DELETE FROM review WHERE review_id = :review_id";

    MapSqlParameterSource params = new MapSqlParameterSource().addValue("review_id", reviewId);
    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean deleted = rowsAffected > 0;

    if (deleted) {
      LOGGER.info("Deleted record for review {}", reviewId);
    } else {
      LOGGER.info("No record found for delete review {}", reviewId);
    }
    // Sampled primary entity ids will cascade delete.
    return deleted;
  }

  /** Fetch all reviews for a cohort. */
  @ReadTransaction
  public List<Review> getAllReviews(
      String studyId, String cohortRevisionGroupId, int offset, int limit) {
    String sql =
        REVIEW_SELECT_SQL
            + " WHERE c.cohort_revision_group_id = :cohort_revision_group_id ORDER BY r.display_name OFFSET :offset LIMIT :limit";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_revision_group_id", cohortRevisionGroupId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return populateCohorts(studyId, jdbcTemplate.query(sql, params, REVIEW_ROW_MAPPER));
  }

  /** Fetch all reviews for a cohort. Only include reviews whose ids are in the specified list. */
  @ReadTransaction
  public List<Review> getReviewsMatchingList(
      String studyId,
      String cohortRevisionGroupId,
      Set<String> reviewIdList,
      int offset,
      int limit) {
    // If the incoming list is empty, the caller does not have permission to see any
    // reviews, so we return an empty list.
    if (reviewIdList.isEmpty()) {
      return Collections.emptyList();
    }
    String sql =
        REVIEW_SELECT_SQL
            + " WHERE r.review_id IN (:review_ids) ORDER BY r.display_name OFFSET :offset LIMIT :limit";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("review_ids", reviewIdList)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return populateCohorts(studyId, jdbcTemplate.query(sql, params, REVIEW_ROW_MAPPER));
  }

  private List<Review> populateCohorts(String studyId, List<Review.Builder> builders) {
    Set<String> cohortIds =
        builders.stream().map(Review.Builder::getCohortId).collect(Collectors.toSet());
    List<Cohort> cohorts = cohortDao.getCohortsMatchingList(studyId, cohortIds);
    return builders.stream()
        .map(
            review ->
                review
                    .cohort(
                        cohorts.stream()
                            .filter(cohort -> cohort.getCohortId().equals(review.getCohortId()))
                            .findFirst()
                            .get())
                    .build())
        .collect(Collectors.toList());
  }

  @ReadTransaction
  public Optional<Review> getReviewIfExists(
      String studyId, String cohortRevisionGroupId, String reviewId) {
    if (studyId == null || cohortRevisionGroupId == null || reviewId == null) {
      throw new MissingRequiredFieldException("Valid study, cohort, and review ids are required");
    }
    String sql = REVIEW_SELECT_SQL + " WHERE r.review_id = :review_id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("review_id", reviewId);
    try {
      Review.Builder review =
          DataAccessUtils.requiredSingleResult(jdbcTemplate.query(sql, params, REVIEW_ROW_MAPPER));
      review.cohort(cohortDao.getCohortVersionOrThrow(studyId, review.getCohortId()));
      LOGGER.info("Retrieved review record {}", review);
      return Optional.of(review.build());
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public Review getReview(String studyId, String cohortRevisionGroupId, String reviewId) {
    return getReviewIfExists(studyId, cohortRevisionGroupId, reviewId)
        .orElseThrow(() -> new NotFoundException(String.format("Review %s not found.", reviewId)));
  }

  @WriteTransaction
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public boolean updateReview(
      String studyId,
      String cohortRevisionGroupId,
      String reviewId,
      @Nullable String name,
      @Nullable String description) {
    if (name == null && description == null) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    MapSqlParameterSource params = new MapSqlParameterSource().addValue("review_id", reviewId);
    if (name != null) {
      params.addValue("display_name", name);
    }
    if (description != null) {
      params.addValue("description", description);
    }

    String sql =
        String.format(
            "UPDATE review SET %s WHERE review_id = :review_id", DbUtils.setColumnsClause(params));

    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean updated = rowsAffected > 0;
    LOGGER.info(
        "{} record for review {}", updated ? "Updated" : "No Update - did not find", reviewId);
    return updated;
  }

  @ReadTransaction
  public List<Literal> getPrimaryEntityIds(
      String studyId, String cohortRevisionGroupId, String reviewId) {
    String sql = REVIEW_INSTANCE_SELECT_SQL + " WHERE review_id = :review_id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("review_id", reviewId);
    return jdbcTemplate.query(sql, params, REVIEW_INSTANCE_ROW_MAPPER);
  }
}
