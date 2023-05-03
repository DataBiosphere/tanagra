package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.service.model.AnnotationKey;
import bio.terra.tanagra.service.model.AnnotationValue;
import java.sql.Array;
import java.util.*;
import java.util.stream.Collectors;
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

  // SQL query and row mapper for reading an annotation value.
  private static final String ANNOTATION_VALUE_SELECT_SQL =
      "SELECT cr.version, av.annotation_key_id, av.primary_entity_instance_id, av.bool_val, av.int64_val, av.string_val, av.date_val, ak.data_type "
          + "FROM annotation_value AS av "
          + "JOIN annotation_key AS ak ON ak.id = av.annotation_key_id AND ak.cohort_id = av.cohort_id "
          + "JOIN cohort_revision AS cr ON cr.review_id = av.review_id";

  private static final RowMapper<AnnotationValue.Builder> ANNOTATION_VALUE_ROW_MAPPER =
      (rs, rowNum) ->
          AnnotationValue.builder()
              .cohortRevisionVersion(rs.getInt("version"))
              .annotationKeyId(rs.getString("annotation_key_id"))
              .instanceId(rs.getString("primary_entity_instance_id"))
              .literal(
                  new Literal.Builder()
                      .booleanVal(rs.getBoolean("bool_val"))
                      .int64Val(rs.getLong("int64_val"))
                      .stringVal(rs.getString("string_val"))
                      .dateVal(rs.getDate("date_val"))
                      .dataType(Literal.DataType.valueOf(rs.getString("data_type")))
                      .build());

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

  @WriteTransaction
  public void updateAnnotationValues(
      String cohortId,
      String annotationKeyId,
      String reviewId,
      String instanceId,
      List<Literal> annotationValues) {
    // Delete any existing annotation values.
    String sql =
        "DELETE FROM annotation_value WHERE cohort_id = :cohort_id AND annotation_key_id = :annotation_key_id AND review_id = :review_id AND primary_entity_instance_id = :primary_entity_instance_id";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohortId)
            .addValue("annotation_key_id", annotationKeyId)
            .addValue("review_id", reviewId)
            .addValue("primary_entity_instance_id", instanceId);
    LOGGER.debug("DELETE annotation values: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE annotation values rowsAffected = {}", rowsAffected);

    // Write the annotation values.
    sql =
        "INSERT INTO annotation_value (cohort_id, annotation_key_id, review_id, primary_entity_instance_id, bool_val, int64_val, string_val, date_val) "
            + "VALUES (:cohort_id, :annotation_key_id, :review_id, :primary_entity_instance_id, :bool_val, :int64_val, :string_val, :date_val)";
    LOGGER.debug("CREATE annotation values: {}", sql);
    List<MapSqlParameterSource> valueParamSets =
        annotationValues.stream()
            .map(
                av ->
                    new MapSqlParameterSource()
                        .addValue("cohort_id", cohortId)
                        .addValue("annotation_key_id", annotationKeyId)
                        .addValue("review_id", reviewId)
                        .addValue("primary_entity_instance_id", instanceId)
                        .addValue("bool_val", av.getBooleanVal())
                        .addValue("int64_val", av.getInt64Val())
                        .addValue("string_val", av.getStringVal())
                        .addValue("date_val", av.getDateVal()))
            .collect(Collectors.toList());
    rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(sql, valueParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE annotation values rowsAffected = {}", rowsAffected);
  }

  @ReadTransaction
  public List<AnnotationValue.Builder> getAllAnnotationValues(String cohortId) {
    String sql = ANNOTATION_VALUE_SELECT_SQL + " WHERE av.cohort_id = :cohort_id";
    LOGGER.debug("GET annotation values: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("cohort_id", cohortId);
    return jdbcTemplate.query(sql, params, ANNOTATION_VALUE_ROW_MAPPER);
  }
}
