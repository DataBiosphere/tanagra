package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.Review;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReviewDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewDao.class);

  // SQL query and row mapper for reading a review.
  private static final String REVIEW_SELECT_SQL =
      "SELECT id, size, display_name, description, created, created_by, last_modified, last_modified_by, is_deleted FROM review";
  private static final RowMapper<Review.Builder> REVIEW_ROW_MAPPER =
      (rs, rowNum) ->
          Review.builder()
              .id(rs.getString("id"))
              .size(rs.getInt("size"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .created(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
              .createdBy(rs.getString("created_by"))
              .lastModified(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
              .lastModifiedBy(rs.getString("last_modified_by"))
              .isDeleted(rs.getBoolean("is_deleted"));

  // SQL query and row mapper for reading a cohort revision.
  private static final String COHORT_REVISION_SELECT_SQL =
      "SELECT review_id, id, version, is_most_recent, is_editable, created, created_by, last_modified, last_modified_by, records_count FROM cohort_revision";
  private static final RowMapper<Pair<String, CohortRevision.Builder>> COHORT_REVISION_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              rs.getString("review_id"),
              CohortRevision.builder()
                  .id(rs.getString("id"))
                  .version(rs.getInt("version"))
                  .setIsMostRecent(rs.getBoolean("is_most_recent"))
                  .setIsEditable(rs.getBoolean("is_editable"))
                  .created(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
                  .createdBy(rs.getString("created_by"))
                  .lastModified(
                      JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
                  .lastModifiedBy(rs.getString("last_modified_by"))
                  .recordsCount(rs.getObject("records_count", Long.class)));

  // SQL query and row mapper for reading a primary entity instance.
  private static final String PRIMARY_ENTITY_INSTANCE_SELECT_SQL =
      "SELECT id, stable_index FROM primary_entity_instance";
  private static final RowMapper<Pair<Literal, Integer>> PRIMARY_ENTITY_INSTANCE_ROW_MAPPER =
      (rs, rowNum) -> Pair.of(Literal.forInt64(rs.getLong("id")), rs.getInt("stable_index"));

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final CohortDao cohortDao;

  @Autowired
  public ReviewDao(NamedParameterJdbcTemplate jdbcTemplate, CohortDao cohortDao) {
    this.jdbcTemplate = jdbcTemplate;
    this.cohortDao = cohortDao;
  }

  @ReadTransaction
  public List<Review> getAllReviews(String cohortId, int offset, int limit) {
    String sql =
        REVIEW_SELECT_SQL
            + " WHERE cohort_id = :cohort_id AND NOT is_deleted ORDER BY created DESC LIMIT :limit OFFSET :offset";
    LOGGER.debug("GET ALL reviews: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohortId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    List<Review> reviews = getReviewsHelper(sql, params);
    LOGGER.debug("GET ALL reviews numFound = {}", reviews.size());
    return reviews;
  }

  @ReadTransaction
  public List<Review> getReviewsMatchingList(Set<String> ids, int offset, int limit) {
    String sql =
        REVIEW_SELECT_SQL
            + " WHERE id IN (:ids) AND NOT is_deleted ORDER BY created DESC LIMIT :limit OFFSET :offset";
    LOGGER.debug("GET MATCHING reviews: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("ids", ids)
            .addValue("offset", offset)
            .addValue("limit", limit);
    List<Review> reviews = getReviewsHelper(sql, params);
    LOGGER.debug("GET MATCHING reviews numFound = {}", reviews.size());
    return reviews;
  }

  @ReadTransaction
  public Review getReview(String id) {
    // Fetch review.
    String sql = REVIEW_SELECT_SQL + " WHERE id = :id";
    LOGGER.debug("GET review: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    List<Review> reviews = getReviewsHelper(sql, params);
    LOGGER.debug("GET review numFound = {}", reviews.size());

    // Make sure there's only one review returned for this id.
    if (reviews.isEmpty()) {
      throw new NotFoundException("Review not found " + id);
    } else if (reviews.size() > 1) {
      throw new SystemException("Multiple reviews found " + id);
    }
    return reviews.get(0);
  }

  @WriteTransaction
  public void deleteReview(String id) {
    String sql = "UPDATE review SET is_deleted = true WHERE id = :id";
    LOGGER.debug("DELETE review: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE review rowsAffected = {}", rowsAffected);
  }

  @WriteTransaction
  public void createReview(
      String cohortId, Review review, List<Long> primaryEntityIds, long recordsCount) {
    // Write the review. The created and last_modified fields are set by the DB automatically on
    // insert.
    String sql =
        "INSERT INTO review (cohort_id, id, size, display_name, description, created_by, last_modified_by, is_deleted) "
            + "VALUES (:cohort_id, :id, :size, :display_name, :description, :created_by, :last_modified_by, false)";
    LOGGER.debug("CREATE review: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohortId)
            .addValue("id", review.getId())
            .addValue("size", review.getSize())
            .addValue("display_name", review.getDisplayName())
            .addValue("description", review.getDescription())
            .addValue("created_by", review.getCreatedBy())
            .addValue("last_modified_by", review.getLastModifiedBy());
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("CREATE review rowsAffected = {}", rowsAffected);

    // Make the current cohort revision un-editable, and create the next version.
    cohortDao.createNextRevision(cohortId, review.getId(), review.getCreatedBy(), recordsCount);

    // Write the primary entity instance ids contained in the review.
    sql =
        "INSERT INTO primary_entity_instance (review_id, id, stable_index) VALUES (:review_id, :id, :stable_index)";
    LOGGER.debug("CREATE primary_entity_instance: {}", sql);
    List<MapSqlParameterSource> paramSets = new ArrayList<>();
    int stableIndex = 0;
    for (Long primaryEntityId : primaryEntityIds) {
      paramSets.add(
          new MapSqlParameterSource()
              .addValue("review_id", review.getId())
              .addValue("id", primaryEntityId)
              .addValue("stable_index", stableIndex));
      stableIndex++;
    }
    rowsAffected =
        IntStream.of(jdbcTemplate.batchUpdate(sql, paramSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE primary_entity_instance rowsAffected = {}", rowsAffected);
  }

  @WriteTransaction
  public void updateReview(
      String id, String lastModifiedBy, String displayName, String description) {
    if (displayName == null && description == null) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    // Check to make sure the review isn't deleted.
    Review review = getReview(id);
    if (review.isDeleted()) {
      throw new NotFoundException("Review " + id + " has been deleted.");
    }

    // Update the review: display name, description, last modified, last modified by.
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("last_modified", JdbcUtils.sqlTimestampUTC())
            .addValue("last_modified_by", lastModifiedBy);
    if (displayName != null) {
      params.addValue("display_name", displayName);
    }
    if (description != null) {
      params.addValue("description", description);
    }
    String sql =
        String.format("UPDATE review SET %s WHERE id = :id", JdbcUtils.setColumnsClause(params));
    LOGGER.debug("UPDATE review: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE review rowsAffected = {}", rowsAffected);
  }

  @ReadTransaction
  public Map<Literal, Integer> getPrimaryEntityIdsToStableIndex(String reviewId) {
    String sql =
        PRIMARY_ENTITY_INSTANCE_SELECT_SQL
            + " WHERE review_id = :review_id ORDER BY stable_index ASC";
    LOGGER.debug("GET primary entity instance ids: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("review_id", reviewId);
    List<Pair<Literal, Integer>> idIndexPairs =
        jdbcTemplate.query(sql, params, PRIMARY_ENTITY_INSTANCE_ROW_MAPPER);
    return idIndexPairs.stream()
        .collect(Collectors.toMap(pair -> pair.getKey(), pair -> pair.getValue()));
  }

  private List<Review> getReviewsHelper(String reviewsSql, MapSqlParameterSource reviewsParams) {
    // Fetch reviews.
    List<Review.Builder> reviews = jdbcTemplate.query(reviewsSql, reviewsParams, REVIEW_ROW_MAPPER);
    if (reviews.isEmpty()) {
      return Collections.emptyList();
    }

    // Fetch the cohort revision for each review. (review id -> cohort revision)
    String sql = COHORT_REVISION_SELECT_SQL + " WHERE review_id IN (:review_ids)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(
                "review_ids", reviews.stream().map(r -> r.getId()).collect(Collectors.toSet()));
    List<Pair<String, CohortRevision.Builder>> cohortRevisions =
        jdbcTemplate.query(sql, params, COHORT_REVISION_ROW_MAPPER);

    // Populate the criteria.
    cohortDao.getCriteriaHelper(cohortRevisions);

    // Put cohort revisions into their respective reviews.
    Map<String, Review.Builder> reviewsMap =
        reviews.stream().collect(Collectors.toMap(Review.Builder::getId, Function.identity()));
    cohortRevisions.stream()
        .forEach(
            entry -> {
              String reviewId = entry.getKey();
              CohortRevision cohortRevision = entry.getValue().build();
              reviewsMap.get(reviewId).revision(cohortRevision);
            });

    // Preserve the order returned by the original query.
    return reviews.stream()
        .map(r -> reviewsMap.get(r.getId()).build())
        .collect(Collectors.toList());
  }
}
