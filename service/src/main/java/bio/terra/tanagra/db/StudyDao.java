package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.db.exception.DuplicateStudyException;
import bio.terra.tanagra.service.artifact.Study;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
public class StudyDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(StudyDao.class);

  // SQL query and row mapper for reading a study.
  private static final String STUDY_SELECT_SQL =
      "SELECT study_id, display_name, description, properties FROM study";
  private static final RowMapper<Study> STUDY_ROW_MAPPER =
      (rs, rowNum) ->
          Study.builder()
              .studyId(rs.getString("study_id"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .properties(
                  Optional.ofNullable(rs.getString("properties"))
                      .map(DbSerDes::jsonToProperties)
                      .orElse(null))
              .build();

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public StudyDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Persists a study to DB.
   *
   * @param study all properties of the study to create
   */
  @WriteTransaction
  public void createStudy(Study study) {
    final String sql =
        "INSERT INTO study (study_id, display_name, description, properties) "
            + "VALUES (:study_id, :display_name, :description, CAST(:properties AS jsonb))";

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", study.getStudyId())
            .addValue("display_name", study.getDisplayName())
            .addValue("description", study.getDescription())
            .addValue("properties", DbSerDes.propertiesToJson(study.getProperties()));
    try {
      jdbcTemplate.update(sql, params);
      LOGGER.info("Inserted record for study {}", study.getStudyId());
    } catch (DuplicateKeyException dkEx) {
      if (dkEx.getMessage()
          .contains("duplicate key value violates unique constraint \"study_pkey\"")) {
        throw new DuplicateStudyException(
            String.format(
                "Study with id %s already exists - display name %s",
                study.getStudyId(), study.getDisplayName()),
            dkEx);
      } else {
        throw dkEx;
      }
    }
  }

  /**
   * @param studyId unique identifier of the study
   * @return true on successful delete, false if there's nothing to delete
   */
  @WriteTransaction
  public boolean deleteStudy(String studyId) {
    final String sql = "DELETE FROM study WHERE study_id = :study_id";

    MapSqlParameterSource params = new MapSqlParameterSource().addValue("study_id", studyId);
    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean deleted = rowsAffected > 0;

    if (deleted) {
      LOGGER.info("Deleted record for study {}", studyId);
    } else {
      LOGGER.info("No record found for delete study {}", studyId);
    }

    return deleted;
  }

  /**
   * Retrieve all studies.
   *
   * @param offset The number of items to skip before starting to collect the result set.
   * @param limit The maximum number of items to return.
   * @return list of all studies
   */
  @ReadTransaction
  public List<Study> getAllStudies(int offset, int limit) {
    String sql = STUDY_SELECT_SQL + " ORDER BY display_name OFFSET :offset LIMIT :limit";
    var params = new MapSqlParameterSource().addValue("offset", offset).addValue("limit", limit);
    return jdbcTemplate.query(sql, params, STUDY_ROW_MAPPER);
  }

  /**
   * Retrieve studies from a list of IDs. IDs not matching studies will be ignored.
   *
   * @param studyIdList List of study ids to query for
   * @param offset The number of items to skip before starting to collect the result set.
   * @param limit The maximum number of items to return.
   * @return list of studies corresponding to input IDs.
   */
  @ReadTransaction
  public List<Study> getStudiesMatchingList(Set<String> studyIdList, int offset, int limit) {
    // If the incoming list is empty, the caller does not have permission to see any
    // studies, so we return an empty list.
    if (studyIdList.isEmpty()) {
      return Collections.emptyList();
    }
    String sql =
        STUDY_SELECT_SQL
            + " WHERE study_id IN (:study_ids) ORDER BY display_name OFFSET :offset LIMIT :limit";
    var params =
        new MapSqlParameterSource()
            .addValue("study_ids", studyIdList)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return jdbcTemplate.query(sql, params, STUDY_ROW_MAPPER);
  }

  @ReadTransaction
  public Optional<Study> getStudyIfExists(String studyId) {
    if (studyId == null) {
      throw new MissingRequiredFieldException("Valid study id is required");
    }
    String sql = STUDY_SELECT_SQL + " WHERE study_id = :study_id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("study_id", studyId);
    try {
      Study result =
          DataAccessUtils.requiredSingleResult(jdbcTemplate.query(sql, params, STUDY_ROW_MAPPER));
      LOGGER.info("Retrieved study record {}", result);
      return Optional.of(result);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  /**
   * Retrieves a study from database by ID.
   *
   * @param studyId unique identifier of the study
   * @return study object
   */
  public Study getStudy(String studyId) {
    return getStudyIfExists(studyId)
        .orElseThrow(() -> new NotFoundException(String.format("Study %s not found.", studyId)));
  }

  @WriteTransaction
  public boolean updateStudy(String studyId, @Nullable String name, @Nullable String description) {
    if (name == null && description == null) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    var params = new MapSqlParameterSource().addValue("study_id", studyId);

    if (name != null) {
      params.addValue("display_name", name);
    }

    if (description != null) {
      params.addValue("description", description);
    }

    String sql =
        String.format(
            "UPDATE study SET %s WHERE study_id = :study_id",
            DbUtils.setColumnsClause(params, "properties"));

    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean updated = rowsAffected > 0;
    LOGGER.info(
        "{} record for study {}", updated ? "Updated" : "No Update - did not find", studyId);
    return updated;
  }

  /** Update a study's properties */
  @WriteTransaction
  public void updateStudyProperties(String studyId, Map<String, String> propertyMap) {
    // Get current property for this study ID.
    String selectPropertiesSql = "SELECT properties FROM study WHERE study_id = :study_id";
    MapSqlParameterSource propertiesParams =
        new MapSqlParameterSource().addValue("study_id", studyId);
    String result;

    try {
      result = jdbcTemplate.queryForObject(selectPropertiesSql, propertiesParams, String.class);
      LOGGER.info("Retrieved study properties {}", result);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException(String.format("Study %s not found.", studyId), e);
    }

    Map<String, String> properties =
        result == null ? new HashMap<>() : DbSerDes.jsonToProperties(result);
    properties.putAll(propertyMap);
    final String sql =
        "UPDATE study SET properties = cast(:properties AS jsonb) WHERE study_id = :study_id";

    var params = new MapSqlParameterSource();
    params
        .addValue("properties", DbSerDes.propertiesToJson(properties))
        .addValue("study_id", studyId);
    jdbcTemplate.update(sql, params);
  }

  @WriteTransaction
  public void deleteStudyProperties(String studyId, List<String> propertyKeys) {
    // Get current property for this study ID.
    String selectPropertiesSql = "SELECT properties FROM study WHERE study_id = :study_id";
    MapSqlParameterSource propertiesParams =
        new MapSqlParameterSource().addValue("study_id", studyId);
    String result;

    try {
      result = jdbcTemplate.queryForObject(selectPropertiesSql, propertiesParams, String.class);
      LOGGER.info("Retrieved study properties {}", result);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException(String.format("Study %s not found.", studyId), e);
    }
    Map<String, String> properties =
        result == null ? new HashMap<>() : DbSerDes.jsonToProperties(result);
    for (String key : propertyKeys) {
      properties.remove(key);
    }
    final String sql =
        "UPDATE study SET properties = cast(:properties AS jsonb) WHERE study_id = :study_id";

    var params = new MapSqlParameterSource();
    params
        .addValue("properties", DbSerDes.propertiesToJson(properties))
        .addValue("study_id", studyId);

    jdbcTemplate.update(sql, params);
  }
}
