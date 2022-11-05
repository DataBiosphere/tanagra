package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.artifact.Study;
import bio.terra.tanagra.exception.DuplicateStudyException;
import java.util.Optional;
import java.util.UUID;
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
public class ArtifactStorageDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactStorageDao.class);

  // SQL query and row mapper for reading a study.
  private static final String STUDY_SELECT_SQL =
      "SELECT study_id, display_name, description, properties FROM study";
  private static final RowMapper<Study> STUDY_ROW_MAPPER =
      (rs, rowNum) ->
          Study.builder()
              .studyId(UUID.fromString(rs.getString("study_id")))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .properties(
                  Optional.ofNullable(rs.getString("properties"))
                      .map(DbSerDes::jsonToProperties)
                      .orElse(null))
              .build();

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public ArtifactStorageDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Persists a study to DB. Returns ID of persisted study on success.
   *
   * @param study all properties of the study to create
   * @return study id
   */
  @WriteTransaction
  public UUID createStudy(Study study) {
    final String sql =
        "INSERT INTO study (study_id, display_name, description, properties) "
            + "VALUES (:study_id, :display_name, :description, CAST(:properties AS jsonb))";

    final String studyUuid = study.getStudyId().toString();
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyUuid)
            .addValue("display_name", study.getDisplayName().orElse(null))
            .addValue("description", study.getDescription().orElse(null))
            .addValue("properties", DbSerDes.propertiesToJson(study.getProperties()));
    try {
      jdbcTemplate.update(sql, params);
      LOGGER.info("Inserted record for study {}", studyUuid);
    } catch (DuplicateKeyException dkEx) {
      if (dkEx.getMessage()
          .contains("duplicate key value violates unique constraint \"study_pkey\"")) {
        throw new DuplicateStudyException(
            String.format(
                "Study with id %s already exists - display name %s",
                studyUuid, study.getDisplayName().orElse(null)),
            dkEx);
      } else {
        throw dkEx;
      }
    }
    return study.getStudyId();
  }

  /**
   * @param studyUuid unique identifier of the study
   * @return true on successful delete, false if there's nothing to delete
   */
  @WriteTransaction
  public boolean deleteStudy(UUID studyUuid) {
    final String sql = "DELETE FROM study WHERE study_id = :id";

    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", studyUuid.toString());
    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean deleted = rowsAffected > 0;

    if (deleted) {
      LOGGER.info("Deleted record for study {}", studyUuid);
    } else {
      LOGGER.info("No record found for delete study {}", studyUuid);
    }

    return deleted;
  }

  @ReadTransaction
  public Optional<Study> getStudyIfExists(UUID studyUuid) {
    if (studyUuid == null) {
      throw new MissingRequiredFieldException("Valid study id is required");
    }
    String sql = STUDY_SELECT_SQL + " WHERE study_id = :id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", studyUuid.toString());
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
   * @param studyUuid unique identifier of the study
   * @return study value object
   */
  public Study getStudy(UUID studyUuid) {
    return getStudyIfExists(studyUuid)
        .orElseThrow(
            () ->
                new NotFoundException(String.format("Study %s not found.", studyUuid.toString())));
  }

  @WriteTransaction
  public boolean updateStudy(UUID studyUuid, @Nullable String name, @Nullable String description) {
    if (name == null && description == null) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    var params = new MapSqlParameterSource();
    params.addValue("study_id", studyUuid.toString());

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
        "{} record for study {}", updated ? "Updated" : "No Update - did not find", studyUuid);
    return updated;
  }
}
