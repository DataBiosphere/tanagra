package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.model.AnnotationKey;
import java.sql.Array;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnnotationDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationDao.class);

  // SQL query and row mapper for reading an annotation key.
  private static final String ANNOTATION_KEY_SELECT_SQL =
      "SELECT id, display_name, description, data_type, enum_vals FROM annotation_key";
  private static final RowMapper<AnnotationKey> ANNOTATION_KEY_ROW_MAPPER =
      (rs, rowNum) ->
          AnnotationKey.builder()
              .id(rs.getString("id"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .dataType(Literal.DataType.valueOf(rs.getString("data_type")))
              .enumVals(List.of((String[]) rs.getArray("enum_vals").getArray()))
              .build();

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public AnnotationDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @WriteTransaction
  public void createAnnotationKey(String cohortId, AnnotationKey annotationKey) {
    String sql =
        "INSERT INTO annotation_key (cohort_id, id, display_name, description, data_type, enum_vals) "
            + "VALUES (:cohort_id, :id, :display_name, :description, :data_type, :enum_vals)";
    LOGGER.debug("CREATE annotation key: {}", sql);
    Array enumValsArr =
        jdbcTemplate
            .getJdbcOperations()
            .execute(
                (ConnectionCallback<Array>)
                    con ->
                        con.createArrayOf(
                            "text", annotationKey.getEnumVals().toArray(new String[0])));
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohortId)
            .addValue("id", annotationKey.getId())
            .addValue("display_name", annotationKey.getDisplayName())
            .addValue("description", annotationKey.getDescription())
            .addValue("data_type", annotationKey.getDataType().name())
            .addValue("enum_vals", enumValsArr);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("CREATE annotation key rowsAffected = {}", rowsAffected);
  }

  @WriteTransaction
  public void deleteAnnotationKey(String cohortId, String annotationKeyId) {
    String sql = "DELETE FROM annotation_key WHERE cohort_id = :cohort_id AND id = :id";
    LOGGER.debug("DELETE annotation key: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("cohort_id", cohortId).addValue("id", annotationKeyId);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE annotation key rowsAffected = {}", rowsAffected);
  }

  @ReadTransaction
  public List<AnnotationKey> getAllAnnotationKeys(String cohortId, int offset, int limit) {
    String sql =
        ANNOTATION_KEY_SELECT_SQL
            + " WHERE cohort_id = :cohort_id ORDER BY display_name OFFSET :offset LIMIT :limit";
    LOGGER.debug("GET all annotation keys: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohortId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return jdbcTemplate.query(sql, params, ANNOTATION_KEY_ROW_MAPPER);
  }

  @ReadTransaction
  public List<AnnotationKey> getAnnotationKeysMatchingList(
      String cohortId, Set<String> annotationKeyIdList, int offset, int limit) {
    // If the incoming list is empty, the caller does not have permission to see any
    // annotations, so we return an empty list.
    if (annotationKeyIdList.isEmpty()) {
      return Collections.emptyList();
    }
    String sql =
        ANNOTATION_KEY_SELECT_SQL
            + " WHERE cohort_id = :cohort_id AND id IN (:ids) ORDER BY display_name OFFSET :offset LIMIT :limit";
    LOGGER.debug("GET matching annotation keys: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohortId)
            .addValue("ids", annotationKeyIdList)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return jdbcTemplate.query(sql, params, ANNOTATION_KEY_ROW_MAPPER);
  }

  @ReadTransaction
  public AnnotationKey getAnnotationKey(String cohortId, String annotationKeyId) {
    String sql = ANNOTATION_KEY_SELECT_SQL + " WHERE cohort_id = :cohort_id AND id = :id";
    LOGGER.debug("GET annotation key: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("cohort_id", cohortId).addValue("id", annotationKeyId);
    List<AnnotationKey> annotationKeys = jdbcTemplate.query(sql, params, ANNOTATION_KEY_ROW_MAPPER);
    LOGGER.debug("GET annotation key numFound = {}", annotationKeys.size());
    if (annotationKeys.isEmpty()) {
      throw new NotFoundException("Annotation key not found: " + cohortId + ", " + annotationKeys);
    } else {
      return annotationKeys.get(0);
    }
  }

  @WriteTransaction
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  public void updateAnnotationKey(
      String cohortId,
      String annotationKeyId,
      @Nullable String displayName,
      @Nullable String description) {
    if (displayName == null && description == null) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("cohort_id", cohortId).addValue("id", annotationKeyId);
    if (displayName != null) {
      params.addValue("display_name", displayName);
    }
    if (description != null) {
      params.addValue("description", description);
    }
    String sql =
        String.format(
            "UPDATE annotation_key SET %s WHERE cohort_id = :cohort_id AND id = :id",
            DbUtils.setColumnsClause(params));
    LOGGER.debug("UPDATE annotation key: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE annotation key rowsAffected = {}", rowsAffected);
  }
}
