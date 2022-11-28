package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.db.exception.DuplicateAnnotationException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.artifact.Annotation;
import bio.terra.tanagra.service.artifact.Cohort;
import java.sql.Array;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnnotationDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationDao.class);

  // SQL query and row mapper for reading an annotation.
  private static final String ANNOTATION_SELECT_SQL =
      "SELECT a.cohort_id, a.annotation_id, a.display_name, a.description, a.data_type, a.enum_vals FROM annotation AS a "
          + "JOIN cohort AS c ON c.cohort_id = a.cohort_id";
  private static final RowMapper<Annotation> ANNOTATION_ROW_MAPPER =
      (rs, rowNum) ->
          Annotation.builder()
              .cohortId(rs.getString("cohort_id"))
              .annotationId(rs.getString("annotation_id"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .dataType(Literal.DataType.valueOf(rs.getString("data_type")))
              .enumVals(List.of((String[]) rs.getArray("enum_vals").getArray()))
              .build();

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final CohortDao cohortDao;

  @Autowired
  public AnnotationDao(NamedParameterJdbcTemplate jdbcTemplate, CohortDao cohortDao) {
    this.jdbcTemplate = jdbcTemplate;
    this.cohortDao = cohortDao;
  }

  /** Create a new annotation. */
  @WriteTransaction
  public void createAnnotation(
      String studyId, String cohortRevisionGroupId, Annotation annotation) {
    Cohort cohort = cohortDao.getCohortLatestVersionOrThrow(studyId, cohortRevisionGroupId);

    final String sql =
        "INSERT INTO annotation (cohort_id, annotation_id, display_name, description, data_type, enum_vals) "
            + "VALUES (:cohort_id, :annotation_id, :display_name, :description, :data_type, :enum_vals)";
    Array enumValsArr =
        jdbcTemplate
            .getJdbcOperations()
            .execute(
                (ConnectionCallback<Array>)
                    con ->
                        con.createArrayOf("text", annotation.getEnumVals().toArray(new String[0])));
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohort.getCohortId())
            .addValue("annotation_id", annotation.getAnnotationId())
            .addValue("display_name", annotation.getDisplayName())
            .addValue("description", annotation.getDescription())
            .addValue("data_type", annotation.getDataType().toString())
            .addValue("enum_vals", enumValsArr);

    try {
      jdbcTemplate.update(sql, params);
      LOGGER.info("Inserted record for annotation {}", annotation.getAnnotationId());
    } catch (DuplicateKeyException dkEx) {
      if (dkEx.getMessage()
          .contains("duplicate key value violates unique constraint \"annotation_pkey\"")) {
        throw new DuplicateAnnotationException(
            String.format(
                "Annotation with id %s already exists - display name %s",
                annotation.getAnnotationId(), annotation.getDisplayName()),
            dkEx);
      } else {
        throw dkEx;
      }
    }
  }

  /** Delete an annotation. */
  @WriteTransaction
  public boolean deleteAnnotation(
      String studyId, String cohortRevisionGroupId, String annotationId) {
    final String sql = "DELETE FROM annotation WHERE annotation_id = :annotation_id";

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("annotation_id", annotationId);
    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean deleted = rowsAffected > 0;

    if (deleted) {
      LOGGER.info("Deleted annotation for cohort {}", annotationId);
    } else {
      LOGGER.info("No record found for delete annotation {}", annotationId);
    }
    // Annotation value rows will cascade delete.
    return deleted;
  }

  /** Fetch all annotations for a cohort. */
  @ReadTransaction
  public List<Annotation> getAllAnnotations(
      String studyId, String cohortRevisionGroupId, int offset, int limit) {
    String sql =
        ANNOTATION_SELECT_SQL
            + " WHERE c.cohort_revision_group_id = :cohort_revision_group_id ORDER BY a.display_name OFFSET :offset LIMIT :limit";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_revision_group_id", cohortRevisionGroupId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return jdbcTemplate.query(sql, params, ANNOTATION_ROW_MAPPER);
  }

  /**
   * Fetch all annotations for a cohort. Only include annotations whose ids are in the specified
   * list.
   */
  @ReadTransaction
  public List<Annotation> getAnnotationsMatchingList(
      String studyId,
      String cohortRevisionGroupId,
      Set<String> annotationIdList,
      int offset,
      int limit) {
    // If the incoming list is empty, the caller does not have permission to see any
    // annotations, so we return an empty list.
    if (annotationIdList.isEmpty()) {
      return Collections.emptyList();
    }
    String sql =
        ANNOTATION_SELECT_SQL
            + " WHERE a.annotation_id IN (:annotation_ids) ORDER BY a.display_name OFFSET :offset LIMIT :limit";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("annotation_ids", annotationIdList)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return jdbcTemplate.query(sql, params, ANNOTATION_ROW_MAPPER);
  }

  @ReadTransaction
  public Optional<Annotation> getAnnotationIfExists(
      String studyId, String cohortRevisionGroupId, String annotationId) {
    if (studyId == null || cohortRevisionGroupId == null || annotationId == null) {
      throw new MissingRequiredFieldException(
          "Valid study, cohort, and annotation ids are required");
    }
    String sql = ANNOTATION_SELECT_SQL + " WHERE a.annotation_id = :annotation_id";
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("annotation_id", annotationId);
    try {
      Annotation annotation =
          DataAccessUtils.requiredSingleResult(
              jdbcTemplate.query(sql, params, ANNOTATION_ROW_MAPPER));
      LOGGER.info("Retrieved annotation record {}", annotation);
      return Optional.of(annotation);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public Annotation getAnnotation(
      String studyId, String cohortRevisionGroupId, String annotationId) {
    return getAnnotationIfExists(studyId, cohortRevisionGroupId, annotationId)
        .orElseThrow(
            () -> new NotFoundException(String.format("Annotation %s not found.", annotationId)));
  }

  @WriteTransaction
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public boolean updateAnnotation(
      String studyId,
      String cohortRevisionGroupId,
      String annotationId,
      @Nullable String name,
      @Nullable String description) {
    if (name == null && description == null) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("annotation_id", annotationId);
    if (name != null) {
      params.addValue("display_name", name);
    }
    if (description != null) {
      params.addValue("description", description);
    }

    String sql =
        String.format(
            "UPDATE annotation SET %s WHERE annotation_id = :annotation_id",
            DbUtils.setColumnsClause(params));

    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean updated = rowsAffected > 0;
    LOGGER.info(
        "{} record for annotation {}",
        updated ? "Updated" : "No Update - did not find",
        annotationId);
    return updated;
  }
}
