package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.db.exception.DuplicateAnnotationValueException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.artifact.AnnotationValueV1;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
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
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class AnnotationValueDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValueDao.class);

  // SQL query and row mapper for reading an annotation value.
  private static final String ANNOTATION_VALUE_SELECT_CLAUSE =
      "av.review_id, av.annotation_id, av.annotation_value_id, av.entity_instance_id, av.bool_val, av.int64_val, av.string_val, av.date_val, a.data_type ";
  private static final String ANNOTATION_VALUE_SELECT_SQL =
      "SELECT "
          + ANNOTATION_VALUE_SELECT_CLAUSE
          + "FROM annotation AS a, annotation_value AS av "
          + "WHERE a.annotation_id = av.annotation_id ";
  private static final RowMapper<AnnotationValueV1> ANNOTATION_VALUE_ROW_MAPPER =
      (rs, rowNum) ->
          AnnotationValueV1.builder()
              .reviewId(rs.getString("review_id"))
              .annotationId(rs.getString("annotation_id"))
              .annotationValueId(rs.getString("annotation_value_id"))
              .entityInstanceId(rs.getString("entity_instance_id"))
              .literal(
                  new Literal.Builder()
                      .booleanVal(rs.getBoolean("bool_val"))
                      .int64Val(rs.getLong("int64_val"))
                      .stringVal(rs.getString("string_val"))
                      .dateVal(rs.getDate("date_val"))
                      .dataType(Literal.DataType.valueOf(rs.getString("data_type")))
                      .build())
              .build();

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public AnnotationValueDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Create a new annotation value. */
  @WriteTransaction
  public void createAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      AnnotationValueV1 annotationValue) {
    final String sql =
        "INSERT INTO annotation_value (review_id, annotation_id, annotation_value_id, entity_instance_id, bool_val, int64_val, string_val, date_val) "
            + "VALUES (:review_id, :annotation_id, :annotation_value_id, :entity_instance_id, :bool_val, :int64_val, :string_val, :date_val)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("review_id", reviewId)
            .addValue("annotation_id", annotationId)
            .addValue("annotation_value_id", annotationValue.getAnnotationValueId())
            .addValue("entity_instance_id", annotationValue.getEntityInstanceId())
            .addValue("bool_val", annotationValue.getLiteral().getBooleanVal())
            .addValue("int64_val", annotationValue.getLiteral().getInt64Val())
            .addValue("string_val", annotationValue.getLiteral().getStringVal())
            .addValue("date_val", annotationValue.getLiteral().getDateVal());

    try {
      jdbcTemplate.update(sql, params);
      LOGGER.info(
          "Inserted record for annotation value {}", annotationValue.getAnnotationValueId());
    } catch (DuplicateKeyException dkEx) {
      if (dkEx.getMessage()
          .contains("duplicate key value violates unique constraint \"pk_annotation_value_fix\"")) {
        throw new DuplicateAnnotationValueException(
            String.format(
                "Annotation value with id %s already exists",
                annotationValue.getAnnotationValueId()),
            dkEx);
      } else {
        throw dkEx;
      }
    }
  }

  /** Delete an annotation value. */
  @WriteTransaction
  public boolean deleteAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      String annotationValueId) {
    final String sql =
        "DELETE FROM annotation_value WHERE review_id = :review_id AND annotation_id = :annotation_id AND annotation_value_id = :annotation_value_id";

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("annotation_value_id", annotationValueId)
            .addValue("annotation_id", annotationId)
            .addValue("review_id", reviewId);
    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean deleted = rowsAffected > 0;

    if (deleted) {
      LOGGER.info("Deleted annotation value for review {}", annotationValueId);
    } else {
      LOGGER.info("No record found for delete annotation value {}", annotationValueId);
    }
    return deleted;
  }

  private Optional<AnnotationValueV1> getAnnotationValueIfExists(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      String annotationValueId) {
    if (studyId == null
        || cohortRevisionGroupId == null
        || annotationId == null
        || reviewId == null
        || annotationValueId == null) {
      throw new MissingRequiredFieldException(
          "Valid study, cohort, annotation, review, and annotation value ids are required");
    }
    String sql =
        ANNOTATION_VALUE_SELECT_SQL
            + " AND av.review_id = :review_id AND av.annotation_id = :annotation_id AND av.annotation_value_id = :annotation_value_id";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("review_id", reviewId)
            .addValue("annotation_id", annotationId)
            .addValue("annotation_value_id", annotationValueId);
    try {
      AnnotationValueV1 annotationValue =
          DataAccessUtils.requiredSingleResult(
              jdbcTemplate.query(sql, params, ANNOTATION_VALUE_ROW_MAPPER));
      LOGGER.info("Retrieved annotation value record {}", annotationValue);
      return Optional.of(annotationValue);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @ReadTransaction
  public AnnotationValueV1 getAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      String annotationValueId) {
    return getAnnotationValueIfExists(
            studyId, cohortRevisionGroupId, annotationId, reviewId, annotationValueId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Annotation value %s not found.", annotationValueId)));
  }

  /** @return list of pair of review create date and AnnotationValue */
  @ReadTransaction
  public List<Pair<OffsetDateTime, AnnotationValueV1>> getAnnotationValuesForCohort(
      String cohortId) {
    if (cohortId == null) {
      throw new MissingRequiredFieldException("Valid cohort id is required");
    }
    String sql =
        "SELECT "
            + ANNOTATION_VALUE_SELECT_CLAUSE
            + ", r.created AS review_created "
            + "FROM annotation AS a, annotation_value AS av, review AS r "
            + "WHERE a.annotation_id = av.annotation_id AND a.cohort_id = :cohort_id AND r.review_id = av.review_id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("cohort_id", cohortId);
    RowMapper<Pair<OffsetDateTime, AnnotationValueV1>> mapper =
        (rs, rowNum) ->
            Pair.of(
                DbUtils.timestampToOffsetDateTime(rs.getTimestamp("review_created")),
                AnnotationValueV1.builder()
                    .reviewId(rs.getString("review_id"))
                    .annotationId(rs.getString("annotation_id"))
                    .annotationValueId(rs.getString("annotation_value_id"))
                    .entityInstanceId(rs.getString("entity_instance_id"))
                    .literal(
                        new Literal.Builder()
                            .booleanVal(rs.getBoolean("bool_val"))
                            .int64Val(rs.getLong("int64_val"))
                            .stringVal(rs.getString("string_val"))
                            .dateVal(rs.getDate("date_val"))
                            .dataType(Literal.DataType.valueOf(rs.getString("data_type")))
                            .build())
                    .build());
    return jdbcTemplate.query(sql, params, mapper);
  }

  @ReadTransaction
  public List<AnnotationValueV1> getAnnotationValues(String reviewId) {
    if (reviewId == null) {
      throw new MissingRequiredFieldException("Valid study, cohort, and review ids are required");
    }
    String sql = ANNOTATION_VALUE_SELECT_SQL + " AND av.review_id = :review_id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("review_id", reviewId);
    return jdbcTemplate.query(sql, params, ANNOTATION_VALUE_ROW_MAPPER);
  }

  @WriteTransaction
  public boolean updateAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      String annotationValueId,
      Literal literal) {
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("bool_val", literal.getBooleanVal())
            .addValue("int64_val", literal.getInt64Val())
            .addValue("string_val", literal.getStringVal())
            .addValue("date_val", literal.getDateVal());
    String sql =
        String.format(
            "UPDATE annotation_value SET %s WHERE annotation_value_id = :annotation_value_id",
            DbUtils.setColumnsClause(params));
    params.addValue("annotation_value_id", annotationValueId);

    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean updated = rowsAffected > 0;
    LOGGER.info(
        "{} record for annotation value {}",
        updated ? "Updated" : "No Update - did not find",
        annotationValueId);
    return updated;
  }

  @WriteTransaction
  public boolean createUpdateAnnotationValue(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      String reviewId,
      AnnotationValueV1 annotationValue) {
    // Insert a new row or update it if it already exists.
    final String upsertSql =
        "INSERT INTO annotation_value (review_id, annotation_id, annotation_value_id, entity_instance_id, bool_val, int64_val, string_val, date_val) "
            + "VALUES (:review_id, :annotation_id, :annotation_value_id, :entity_instance_id, :bool_val, :int64_val, :string_val, :date_val) "
            + "ON CONFLICT ON CONSTRAINT pk_annotation_value_fix DO "
            + "UPDATE SET bool_val = :bool_val, int64_val = :int64_val, string_val = :string_val, date_val = :date_val";
    MapSqlParameterSource upsertParams =
        new MapSqlParameterSource()
            .addValue("review_id", reviewId)
            .addValue("annotation_id", annotationId)
            .addValue("annotation_value_id", annotationValue.getAnnotationValueId())
            .addValue("entity_instance_id", annotationValue.getEntityInstanceId())
            .addValue("bool_val", annotationValue.getLiteral().getBooleanVal())
            .addValue("int64_val", annotationValue.getLiteral().getInt64Val())
            .addValue("string_val", annotationValue.getLiteral().getStringVal())
            .addValue("date_val", annotationValue.getLiteral().getDateVal());

    int rowsAffected = jdbcTemplate.update(upsertSql, upsertParams);
    LOGGER.info(
        "Updated {} records for annotation value {}",
        rowsAffected,
        annotationValue.getAnnotationValueId());
    return rowsAffected > 0;
  }
}
