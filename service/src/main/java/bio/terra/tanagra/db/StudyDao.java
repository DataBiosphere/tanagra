package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.artifact.model.Study;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudyDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(StudyDao.class);

  // SQL query and row mapper for reading a study.
  private static final String STUDY_SELECT_SQL =
      "SELECT id, display_name, description, created, created_by, last_modified, last_modified_by, is_deleted FROM study";
  private static final RowMapper<Study.Builder> STUDY_ROW_MAPPER =
      (rs, rowNum) ->
          Study.builder()
              .id(rs.getString("id"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .created(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
              .createdBy(rs.getString("created_by"))
              .lastModified(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
              .lastModifiedBy(rs.getString("last_modified_by"))
              .isDeleted(rs.getBoolean("is_deleted"));

  // SQL query and row mapper for reading a property.
  public static final String PROPERTY_SELECT_SQL =
      "SELECT study_id, property_key, property_value FROM study_property";
  public static final RowMapper<Pair<String, Pair<String, String>>> PROPERTY_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              rs.getString("study_id"),
              Pair.of(rs.getString("property_key"), rs.getString("property_value")));
  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public StudyDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @WriteTransaction
  public void createStudy(Study study) {
    String sql =
        "INSERT INTO study (id, display_name, description, created_by, last_modified_by, is_deleted) "
            + "VALUES (:id, :display_name, :description, :created_by, :last_modified_by, false)";
    LOGGER.debug("CREATE study: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", study.getId())
            .addValue("display_name", study.getDisplayName())
            .addValue("description", study.getDescription())
            // Don't need to set created or last_modified. Liquibase defaultValueComputed handles
            // that.
            .addValue("created_by", study.getCreatedBy())
            .addValue("last_modified_by", study.getLastModifiedBy());
    try {
      int rowsAffected = jdbcTemplate.update(sql, params);
      LOGGER.debug("CREATE study rowsAffected = {}", rowsAffected);
    } catch (DuplicateKeyException dkEx) {
      if (dkEx.getMessage()
          .contains("duplicate key value violates unique constraint \"study_pkey\"")) {
        throw new BadRequestException(
            String.format(
                "Study with id %s already exists - display name %s",
                study.getId(), study.getDisplayName()),
            dkEx);
      } else {
        throw dkEx;
      }
    }

    if (!study.getProperties().isEmpty()) {
      updatePropertiesHelper(study.getId(), study.getProperties());
    }
  }

  /**
   * @param id unique identifier of the study
   * @return true on successful delete, false if there's nothing to delete
   */
  @WriteTransaction
  public boolean deleteStudy(String id) {
    String sql = "UPDATE study SET is_deleted = true WHERE id = :id";
    LOGGER.debug("DELETE study: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE study rowsAffected = {}", rowsAffected);
    return rowsAffected == 1;
  }

  /**
   * Retrieve all studies.
   *
   * @param offset The number of items to skip before starting to collect the result set.
   * @param limit The maximum number of items to return.
   * @return list of all studies
   */
  @ReadTransaction
  public List<Study> getAllStudies(
      int offset, int limit, boolean includeDeleted, @Nullable Study.Builder studyFilter) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("offset", offset).addValue("limit", limit);
    String filterSql = renderSqlForStudyFilter(includeDeleted, studyFilter, params);
    String sql =
        STUDY_SELECT_SQL
            + (filterSql.isEmpty() ? "" : " WHERE " + filterSql)
            + " ORDER BY display_name LIMIT :limit OFFSET :offset";
    LOGGER.debug("GET all studies: {}", sql);
    List<Study> studies = getStudiesHelper(sql, params);
    LOGGER.debug("GET all studies numFound = {}", studies.size());
    return studies;
  }

  /**
   * Retrieve studies from a list of IDs. IDs not matching studies will be ignored.
   *
   * @param ids List of study ids to query for
   * @param offset The number of items to skip before starting to collect the result set.
   * @param limit The maximum number of items to return.
   * @return list of studies corresponding to input IDs.
   */
  @ReadTransaction
  public List<Study> getStudiesMatchingList(
      Set<String> ids,
      int offset,
      int limit,
      boolean includeDeleted,
      @Nullable Study.Builder studyFilter) {
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("ids", ids)
            .addValue("offset", offset)
            .addValue("limit", limit);
    String filterSql = renderSqlForStudyFilter(includeDeleted, studyFilter, params);
    String sql =
        STUDY_SELECT_SQL
            + " WHERE id IN (:ids) "
            + (filterSql.isEmpty() ? "" : "AND " + filterSql + " ")
            + "ORDER BY display_name LIMIT :limit OFFSET :offset";
    LOGGER.debug("GET matching studies: {}", sql);
    List<Study> studies = getStudiesHelper(sql, params);
    LOGGER.debug("GET matching studies numFound = {}", studies.size());
    return studies;
  }

  /** Convert a study filter object into a SQL WHERE clause. */
  private String renderSqlForStudyFilter(
      boolean includeDeleted, Study.Builder studyFilter, MapSqlParameterSource params) {
    if (includeDeleted && studyFilter == null) {
      return "";
    }

    // Filter out the deleted studies.
    List<String> whereConditions = new ArrayList<>();
    if (!includeDeleted) {
      whereConditions.add("NOT is_deleted");
    }

    if (studyFilter != null) {
      // Filter on the displayName and/or description.
      if (studyFilter.getDisplayName() != null && !studyFilter.getDisplayName().isEmpty()) {
        whereConditions.add("display_name LIKE :display_name_filter");
        params.addValue("display_name_filter", "%" + studyFilter.getDisplayName() + "%");
      }
      if (studyFilter.getDescription() != null && !studyFilter.getDescription().isEmpty()) {
        whereConditions.add("description LIKE :description_filter");
        params.addValue("description_filter", "%" + studyFilter.getDescription() + "%");
      }
      if (studyFilter.getCreatedBy() != null && !studyFilter.getCreatedBy().isEmpty()) {
        whereConditions.add("created_by LIKE :created_by_filter");
        params.addValue("created_by_filter", "%" + studyFilter.getCreatedBy() + "%");
      }

      // Filter on specific properties key-value pairs.
      if (studyFilter.getProperties() != null && !studyFilter.getProperties().isEmpty()) {
        int ctr = 0;
        for (Map.Entry<String, String> entry : studyFilter.getProperties().entrySet()) {
          whereConditions.add(
              "EXISTS (SELECT 1 FROM study_property WHERE study_id = id AND property_key = :key_"
                  + ctr
                  + " AND property_value LIKE :value_like_"
                  + ctr
                  + ")");
          params.addValue("key_" + ctr, entry.getKey());
          params.addValue("value_like_" + ctr, "%" + entry.getValue() + "%");
          ctr++;
        }
      }
    }

    // Build a WHERE clause ready string.
    return String.join(" AND ", whereConditions);
  }

  @ReadTransaction
  public Optional<Study> getStudyIfExists(String id) {
    // Fetch study.
    String sql = STUDY_SELECT_SQL + " WHERE id = :id";
    LOGGER.debug("GET study: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    List<Study> studies = getStudiesHelper(sql, params);
    LOGGER.debug("GET study numFound = {}", studies.size());

    // Make sure there's only one study returned for this id.
    if (studies.isEmpty()) {
      throw new NotFoundException("Study not found " + id);
    } else if (studies.size() > 1) {
      throw new SystemException("Multiple studies found " + id);
    }
    return Optional.of(studies.get(0));
  }

  public Study getStudy(String id) {
    return getStudyIfExists(id)
        .orElseThrow(() -> new NotFoundException(String.format("Study %s not found.", id)));
  }

  public Study getStudyNotDeleted(String id) {
    Study study = getStudy(id);
    if (study.isDeleted()) {
      throw new NotFoundException("Study " + id + " has been deleted");
    }
    return study;
  }

  @WriteTransaction
  public boolean updateStudy(
      String id, String lastModifiedBy, @Nullable String name, @Nullable String description) {
    // Check to make sure the study isn't deleted.
    getStudyNotDeleted(id);

    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("last_modified_by", lastModifiedBy);
    if (name != null) {
      params.addValue("display_name", name);
    }
    if (description != null) {
      params.addValue("description", description);
    }

    String sql =
        String.format(
            "UPDATE study SET %s, last_modified = :last_modified WHERE id = :id",
            JdbcUtils.setColumnsClause(params));
    params.addValue("id", id);
    params.addValue("last_modified", JdbcUtils.sqlTimestampUTC());

    LOGGER.debug("UPDATE study: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE study rowsAffected = {}", rowsAffected);
    return rowsAffected == 1;
  }

  @WriteTransaction
  public void updateStudyProperties(
      String id, String lastModifiedBy, Map<String, String> propertyMap) {
    // Get the current properties for this study.
    Study study = getStudyNotDeleted(id);

    // Update just the properties specified, leave the rest as is.
    Map<String, String> updatedProperties = new HashMap<>();
    updatedProperties.putAll(study.getProperties());
    updatedProperties.putAll(propertyMap);

    // Write the new properties.
    updatePropertiesHelper(id, updatedProperties);

    // Update the study timestamps.
    updateStudy(id, lastModifiedBy, null, null);
  }

  @WriteTransaction
  public void deleteStudyProperties(String id, String lastModifiedBy, List<String> propertyKeys) {
    // Get the current properties for this study.
    Study study = getStudyNotDeleted(id);

    // Delete just the properties specified, leave the rest as is.
    Map<String, String> updatedProperties = new HashMap<>(study.getProperties());
    for (String key : propertyKeys) {
      updatedProperties.remove(key);
    }

    // Write the new properties.
    updatePropertiesHelper(id, updatedProperties);

    // Update the study timestamps.
    updateStudy(id, lastModifiedBy, null, null);
  }

  @WriteTransaction
  public void deleteAllStudies() {
    LOGGER.warn("Deleting all studies. This should only happen during testing.");
    String sql = "DELETE FROM study";
    LOGGER.debug("DELETE study: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, new MapSqlParameterSource());
    LOGGER.debug("DELETE study rowsAffected = {}", rowsAffected);
  }

  private List<Study> getStudiesHelper(String studiesSql, MapSqlParameterSource studiesParams) {
    // Fetch studies.
    List<Study.Builder> studies = jdbcTemplate.query(studiesSql, studiesParams, STUDY_ROW_MAPPER);
    if (studies.isEmpty()) {
      return Collections.emptyList();
    }

    // Fetch properties. (study id -> property)
    String sql = PROPERTY_SELECT_SQL + " WHERE study_id IN (:study_ids)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(
                "study_ids",
                studies.stream().map(Study.Builder::getId).collect(Collectors.toSet()));
    List<Pair<String, Pair<String, String>>> properties =
        jdbcTemplate.query(sql, params, PROPERTY_ROW_MAPPER);

    // Put properties into their respective studies.
    Map<String, Study.Builder> studiesMap =
        studies.stream().collect(Collectors.toMap(Study.Builder::getId, Function.identity()));
    properties.forEach(
        pair -> {
          String studyId = pair.getKey();
          Pair<String, String> property = pair.getValue();
          studiesMap.get(studyId).addProperty(property.getKey(), property.getValue());
        });

    // Preserve the order returned by the original query.
    return studies.stream()
        .map(s -> studiesMap.get(s.getId()).build())
        .collect(Collectors.toList());
  }

  private void updatePropertiesHelper(String studyId, Map<String, String> properties) {
    // Delete any existing properties.
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("study_id", studyId);
    String sql = "DELETE FROM study_property WHERE study_id = :study_id";
    LOGGER.debug("DELETE study property: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE study property rowsAffected = {}", rowsAffected);

    // Write the properties.
    sql =
        "INSERT INTO study_property (study_id, property_key, property_value) VALUES (:study_id, :key, :value)";
    LOGGER.debug("CREATE study property: {}", sql);
    List<MapSqlParameterSource> propertyParamSets =
        properties.entrySet().stream()
            .map(
                p ->
                    new MapSqlParameterSource()
                        .addValue("study_id", studyId)
                        .addValue("key", p.getKey())
                        .addValue("value", p.getValue()))
            .collect(Collectors.toList());
    rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(
                    sql, propertyParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE study property rowsAffected = {}", rowsAffected);
  }
}
