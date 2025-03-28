package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.AnnotationValue;
import jakarta.annotation.Nullable;
import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnnotationDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationDao.class);

  // SQL query and row mapper for reading an annotation key.
  private static final String ANNOTATION_KEY_SELECT_SQL =
      "SELECT id, display_name, description, data_type FROM annotation_key";
  private static final RowMapper<AnnotationKey.Builder> ANNOTATION_KEY_ROW_MAPPER =
      (rs, rowNum) ->
          AnnotationKey.builder()
              .id(rs.getString("id"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .dataType(DataType.valueOf(rs.getString("data_type")));

  // SQL query and row mapper for reading an annotation key enum value.
  private static final String ANNOTATION_KEY_ENUM_VALUE_SELECT_SQL =
      "SELECT annotation_key_id, enum FROM annotation_key_enum_value";
  private static final RowMapper<Pair<String, String>> ANNOTATION_KEY_ENUM_VALUE_ROW_MAPPER =
      (rs, rowNum) -> Pair.of(rs.getString("annotation_key_id"), rs.getString("enum"));

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
                  Literal.forGeneric(
                      DataType.valueOf(rs.getString("data_type")),
                      rs.getString("string_val"),
                      rs.getLong("int64_val"),
                      rs.getBoolean("bool_val"),
                      rs.getDate("date_val"),
                      null));

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public AnnotationDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @WriteTransaction
  public void createAnnotationKey(String cohortId, AnnotationKey annotationKey) {
    String sql =
        "INSERT INTO annotation_key (cohort_id, id, display_name, description, data_type) "
            + "VALUES (:cohort_id, :id, :display_name, :description, :data_type)";
    MapSqlParameterSource keyParamSets =
        buildKeyParam(
            cohortId,
            annotationKey.getId(),
            annotationKey.getDisplayName(),
            annotationKey.getDescription(),
            annotationKey.getDataType().name());
    JdbcUtils.insertRows(jdbcTemplate, "annotation_key", sql, List.of(keyParamSets));

    if (!annotationKey.getEnumVals().isEmpty()) {
      sql =
          "INSERT INTO annotation_key_enum_value (cohort_id, annotation_key_id, enum) "
              + "VALUES (:cohort_id, :annotation_key_id, :enum)";
      List<MapSqlParameterSource> enumValueParamSets =
          annotationKey.getEnumVals().stream()
              .map(val -> buildEnumValueParam(cohortId, annotationKey.getId(), val))
              .toList();
      JdbcUtils.insertRows(jdbcTemplate, "annotation_key_enum_value", sql, enumValueParamSets);
    }
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
            + " WHERE cohort_id = :cohort_id ORDER BY display_name LIMIT :limit OFFSET :offset";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohortId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return getAnnotationKeysHelper(sql, params);
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
            + " WHERE cohort_id = :cohort_id AND id IN (:ids) ORDER BY display_name LIMIT :limit OFFSET :offset";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohortId)
            .addValue("ids", annotationKeyIdList)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return getAnnotationKeysHelper(sql, params);
  }

  @ReadTransaction
  public AnnotationKey getAnnotationKey(String cohortId, String annotationKeyId) {
    String sql = ANNOTATION_KEY_SELECT_SQL + " WHERE cohort_id = :cohort_id AND id = :id";
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("cohort_id", cohortId).addValue("id", annotationKeyId);
    List<AnnotationKey> annotationKeys = getAnnotationKeysHelper(sql, params);

    // Make sure there's only one annotation key returned for this id.
    if (annotationKeys.isEmpty()) {
      throw new NotFoundException("Annotation key not found " + cohortId + ", " + annotationKeyId);
    } else if (annotationKeys.size() > 1) {
      throw new SystemException(
          "Multiple annotation keys found " + cohortId + ", " + annotationKeyId);
    }
    return annotationKeys.get(0);
  }

  private List<AnnotationKey> getAnnotationKeysHelper(
      String annotationKeysSql, MapSqlParameterSource annotationKeysParams) {
    // Fetch annotation keys.
    List<AnnotationKey.Builder> annotationKeys =
        jdbcTemplate.query(annotationKeysSql, annotationKeysParams, ANNOTATION_KEY_ROW_MAPPER);
    if (annotationKeys.isEmpty()) {
      return Collections.emptyList();
    }

    // Put enum vals into their respective annotation keys.
    Map<String, AnnotationKey.Builder> annotationKeysMap =
        annotationKeys.stream()
            .collect(Collectors.toMap(AnnotationKey.Builder::getId, Function.identity()));

    // Fetch enum vals. (annotation key id -> enum)
    List<Pair<String, String>> enumVals = getEnumValuesMatchingList(annotationKeysMap.keySet());
    enumVals.forEach(pair -> annotationKeysMap.get(pair.getKey()).addEnumVal(pair.getValue()));

    // Preserve the order returned by the original query.
    return annotationKeys.stream()
        .map(a -> annotationKeysMap.get(a.getId()).build())
        .collect(Collectors.toList());
  }

  public List<Pair<String, String>> getEnumValuesMatchingList(Set<String> keyIds) {
    String sql =
        ANNOTATION_KEY_ENUM_VALUE_SELECT_SQL + " WHERE annotation_key_id IN (:annotation_key_ids)";
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("annotation_key_ids", keyIds);
    return jdbcTemplate.query(sql, params, ANNOTATION_KEY_ENUM_VALUE_ROW_MAPPER);
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
            JdbcUtils.setColumnsClause(params));
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
    List<MapSqlParameterSource> valueParamSets =
        annotationValues.stream()
            .map(
                av ->
                    buildValueParam(
                        cohortId,
                        annotationKeyId,
                        reviewId,
                        instanceId,
                        av.getBooleanVal(),
                        av.getInt64Val(),
                        av.getStringVal(),
                        av.getDateVal()))
            .toList();
    JdbcUtils.insertRows(jdbcTemplate, "annotation_value", sql, valueParamSets);
  }

  @ReadTransaction
  public List<AnnotationValue.Builder> getAllAnnotationValues(String cohortId) {
    String sql = ANNOTATION_VALUE_SELECT_SQL + " WHERE av.cohort_id = :cohort_id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("cohort_id", cohortId);
    return jdbcTemplate.query(sql, params, ANNOTATION_VALUE_ROW_MAPPER);
  }

  static MapSqlParameterSource buildKeyParam(
      String cohortId, String id, String displayName, String description, String dataTypeName) {
    return new MapSqlParameterSource()
        .addValue("cohort_id", cohortId)
        .addValue("id", id)
        .addValue("display_name", displayName)
        .addValue("description", description)
        .addValue("data_type", dataTypeName);
  }

  static MapSqlParameterSource buildEnumValueParam(
      String cohortId, String keyId, String enumValue) {
    return new MapSqlParameterSource()
        .addValue("cohort_id", cohortId)
        .addValue("annotation_key_id", keyId)
        .addValue("enum", enumValue);
  }

  static MapSqlParameterSource buildValueParam(
      String cohortId,
      String keyId,
      String reviewId,
      String instanceId,
      Boolean booelanVal,
      Long int64Val,
      String stringVal,
      Date dateVal) {
    return new MapSqlParameterSource()
        .addValue("cohort_id", cohortId)
        .addValue("annotation_key_id", keyId)
        .addValue("review_id", reviewId)
        .addValue("primary_entity_instance_id", instanceId)
        .addValue("bool_val", booelanVal)
        .addValue("int64_val", int64Val)
        .addValue("string_val", stringVal)
        .addValue("date_val", dateVal);
  }
}
