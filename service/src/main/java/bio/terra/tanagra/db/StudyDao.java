package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.service.artifact.Study;
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
      "SELECT id, display_name, description, properties, created, created_by, last_modified, last_modified_by FROM study";
  private static final RowMapper<Study> STUDY_ROW_MAPPER =
      (rs, rowNum) ->
          Study.builder()
              .id(rs.getString("id"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .properties(
                  Optional.ofNullable(rs.getString("properties"))
                      .map(DbSerDes::jsonToProperties)
                      .orElse(null))
              .created(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
              .createdBy(rs.getString("created_by"))
              .lastModified(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
              .lastModifiedBy(rs.getString("last_modified_by"))
              .build();

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public StudyDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @WriteTransaction
  public void createStudy(Study study) {
    String sql =
        "INSERT INTO study (id, display_name, description, properties, created_by, last_modified_by) "
            + "VALUES (:id, :display_name, :description, CAST(:properties AS jsonb), :created_by, :last_modified_by)";
    LOGGER.debug("CREATE study: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", study.getId())
            .addValue("display_name", study.getDisplayName())
            .addValue("description", study.getDescription())
            .addValue("properties", DbSerDes.propertiesToJson(study.getProperties()))
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
  }

  /**
   * @param studyId unique identifier of the study
   * @return true on successful delete, false if there's nothing to delete
   */
  @WriteTransaction
  public boolean deleteStudy(String studyId) {
    String sql = "DELETE FROM study WHERE id = :id";
    LOGGER.debug("DELETE study: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", studyId);
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
  public List<Study> getAllStudies(int offset, int limit) {
    String sql = STUDY_SELECT_SQL + " ORDER BY display_name OFFSET :offset LIMIT :limit";
    LOGGER.debug("GET all studies: {}", sql);
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
    String sql =
        STUDY_SELECT_SQL + " WHERE id IN (:ids) ORDER BY display_name OFFSET :offset LIMIT :limit";
    LOGGER.debug("GET matching studies: {}", sql);
    var params =
        new MapSqlParameterSource()
            .addValue("ids", studyIdList)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return jdbcTemplate.query(sql, params, STUDY_ROW_MAPPER);
  }

  @ReadTransaction
  public Optional<Study> getStudyIfExists(String studyId) {
    if (studyId == null) {
      throw new MissingRequiredFieldException("Valid study id is required");
    }
    String sql = STUDY_SELECT_SQL + " WHERE id = :id";
    LOGGER.debug("GET study: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", studyId);
    try {
      Study result =
          DataAccessUtils.requiredSingleResult(jdbcTemplate.query(sql, params, STUDY_ROW_MAPPER));
      LOGGER.debug("GET study: {}", result);
      return Optional.of(result);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public Study getStudy(String studyId) {
    return getStudyIfExists(studyId)
        .orElseThrow(() -> new NotFoundException(String.format("Study %s not found.", studyId)));
  }

  @WriteTransaction
  public boolean updateStudy(
      String studyId, String lastModifiedBy, @Nullable String name, @Nullable String description) {
    if (name == null && description == null) {
      throw new MissingRequiredFieldException("Study name or description must be not null.");
    }

    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", studyId)
            .addValue("last_modified_by", lastModifiedBy);
    if (name != null) {
      params.addValue("display_name", name);
    }
    if (description != null) {
      params.addValue("description", description);
    }

    String sql =
        String.format(
            "UPDATE study SET %s, last_modified = current_timestamp WHERE id = :id",
            DbUtils.setColumnsClause(params, "properties"));
    LOGGER.debug("UPDATE study: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE study rowsAffected = {}", rowsAffected);
    return rowsAffected == 1;
  }

  @WriteTransaction
  public void updateStudyProperties(
      String studyId, String lastModifiedBy, Map<String, String> propertyMap) {
    // Get current property for this study ID.
    String sql = "SELECT properties FROM study WHERE id = :id";
    LOGGER.debug("GET PROPERTIES study: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", studyId);
    String result;
    try {
      result = jdbcTemplate.queryForObject(sql, params, String.class);
      LOGGER.info("GET PROPERTIES study: {}", result);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException(String.format("Study %s not found.", studyId), e);
    }

    Map<String, String> properties =
        result == null ? new HashMap<>() : DbSerDes.jsonToProperties(result);
    properties.putAll(propertyMap);
    sql =
        "UPDATE study SET properties = cast(:properties AS jsonb), last_modified = current_timestamp, last_modified_by = :last_modified_by WHERE id = :id";
    LOGGER.debug("UPDATE study: {}", sql);
    params =
        new MapSqlParameterSource()
            .addValue("properties", DbSerDes.propertiesToJson(properties))
            .addValue("id", studyId)
            .addValue("last_modified_by", lastModifiedBy);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE study rowsAffected = {}", rowsAffected);
  }

  @WriteTransaction
  public void deleteStudyProperties(
      String studyId, String lastModifiedBy, List<String> propertyKeys) {
    // Get current property for this study ID.
    String sql = "SELECT properties FROM study WHERE id = :id";
    LOGGER.debug("GET PROPERTIES study: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", studyId);
    String result;
    try {
      result = jdbcTemplate.queryForObject(sql, params, String.class);
      LOGGER.debug("GET PROPERTIES study: {}", result);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException(String.format("Study %s not found.", studyId), e);
    }

    Map<String, String> properties =
        result == null ? new HashMap<>() : DbSerDes.jsonToProperties(result);
    for (String key : propertyKeys) {
      properties.remove(key);
    }
    sql =
        "UPDATE study SET properties = cast(:properties AS jsonb), last_modified = current_timestamp, last_modified_by = :last_modified_by WHERE id = :id";
    LOGGER.debug("UPDATE study: {}", sql);
    params =
        new MapSqlParameterSource()
            .addValue("properties", DbSerDes.propertiesToJson(properties))
            .addValue("id", studyId)
            .addValue("last_modified_by", lastModifiedBy);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE study rowsAffected = {}", rowsAffected);
  }
}
