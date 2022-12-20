package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.db.exception.DuplicateCohortException;
import bio.terra.tanagra.db.exception.DuplicateStudyException;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable.LogicalOperator;
import bio.terra.tanagra.service.artifact.Cohort;
import bio.terra.tanagra.service.artifact.Criteria;
import bio.terra.tanagra.service.artifact.CriteriaGroup;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
public class CohortDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(CohortDao.class);

  // SQL query and row mapper for reading a cohort.
  private static final String COHORT_SELECT_SQL =
      "SELECT study_id, cohort_id, underlay_name, cohort_revision_group_id, version, is_most_recent, is_editable, created, last_modified, display_name, description FROM cohort";
  private static final RowMapper<Cohort.Builder> COHORT_ROW_MAPPER =
      (rs, rowNum) ->
          Cohort.builder()
              .studyId(rs.getString("study_id"))
              .cohortId(rs.getString("cohort_id"))
              .underlayName(rs.getString("underlay_name"))
              .cohortRevisionGroupId(rs.getString("cohort_revision_group_id"))
              .version(rs.getInt("version"))
              .isMostRecent(rs.getBoolean("is_most_recent"))
              .isEditable(rs.getBoolean("is_editable"))
              .created(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
              .lastModified(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"));

  // SQL query and row mapper for reading a criteria group.
  private static final String CRITERIA_GROUP_SELECT_SQL =
      "SELECT cohort_id, criteria_group_id, user_facing_criteria_group_id, display_name, operator, is_excluded FROM criteria_group";
  private static final RowMapper<CriteriaGroup.Builder> CRITERIA_GROUP_ROW_MAPPER =
      (rs, rowNum) ->
          CriteriaGroup.builder()
              .cohortId(rs.getString("cohort_id"))
              .criteriaGroupId(rs.getString("criteria_group_id"))
              .userFacingCriteriaGroupId(rs.getString("user_facing_criteria_group_id"))
              .displayName(rs.getString("display_name"))
              .operator(LogicalOperator.valueOf(rs.getString("operator")))
              .isExcluded(rs.getBoolean("is_excluded"));

  // SQL query and row mapper for reading a criteria.
  static final String CRITERIA_SELECT_SQL =
      "SELECT criteria_group_id, concept_set_id, criteria_id, user_facing_criteria_id, display_name, plugin_name, selection_data, ui_config FROM criteria";
  static final RowMapper<Criteria> CRITERIA_ROW_MAPPER =
      (rs, rowNum) ->
          Criteria.builder()
              .criteriaGroupId(rs.getString("criteria_group_id"))
              .conceptSetId(rs.getString("concept_set_id"))
              .criteriaId(rs.getString("criteria_id"))
              .userFacingCriteriaId(rs.getString("user_facing_criteria_id"))
              .displayName(rs.getString("display_name"))
              .pluginName(rs.getString("plugin_name"))
              .selectionData(rs.getString("selection_data"))
              .uiConfig(rs.getString("ui_config"))
              .build();

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public CohortDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Fetch all cohorts that are the most recent version for their user-facing id. */
  @ReadTransaction
  public List<Cohort> getAllCohortsLatestVersion(String studyId, int offset, int limit) {
    return getCohortsHelper(studyId, offset, limit, null, null);
  }

  /**
   * Fetch the latest version of all cohorts. Only returns cohorts with the specified revision group
   * ids.
   */
  @ReadTransaction
  public List<Cohort> getCohortsMatchingListLatestVersion(
      String studyId, Set<String> cohortRevisionGroupIdList, int offset, int limit) {
    return getCohortsHelper(studyId, offset, limit, null, cohortRevisionGroupIdList);
  }

  /**
   * Fetch the cohort versions. Only returns cohorts with the specified ids. Used for cohort
   * reviews, so transaction annotation lives on the ReviewDao methods.
   */
  public List<Cohort> getCohortsMatchingList(String studyId, Set<String> cohortIdList) {
    return getCohortsHelper(studyId, 0, Integer.MAX_VALUE, cohortIdList, null);
  }

  /** Helper method for fetching a list of cohorts. */
  @SuppressWarnings("PMD.InsufficientStringBufferDeclaration")
  private List<Cohort> getCohortsHelper(
      String studyId,
      int offset,
      int limit,
      @Nullable Set<String> cohortIdList,
      @Nullable Set<String> cohortRevisionGroupIdList) {
    if ((cohortIdList != null && cohortIdList.isEmpty())
        || (cohortRevisionGroupIdList != null && cohortRevisionGroupIdList.isEmpty())) {
      // If the incoming list is empty, the caller does not have permission to see any cohorts, so
      // we return an empty list.
      return Collections.emptyList();
    }

    StringBuilder sql =
        new StringBuilder(COHORT_SELECT_SQL + " WHERE study_id = :study_id AND is_most_recent");
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    if (cohortIdList != null) {
      sql.append(" AND cohort_id IN (:cohort_ids)");
      params.addValue("cohort_ids", cohortIdList);
    }
    if (cohortRevisionGroupIdList != null) {
      sql.append(" AND cohort_revision_group_id IN (:cohort_revision_group_ids)");
      params.addValue("cohort_revision_group_ids", cohortRevisionGroupIdList);
    }
    sql.append(" ORDER BY display_name OFFSET :offset LIMIT :limit");
    List<Cohort.Builder> cohorts = jdbcTemplate.query(sql.toString(), params, COHORT_ROW_MAPPER);
    populateCriteriaGroups(cohorts);
    return cohorts.stream().map(Cohort.Builder::build).collect(Collectors.toList());
  }

  /** Fetch the most recent version for the given revision group id. */
  @ReadTransaction
  public Cohort getCohortLatestVersion(String studyId, String cohortRevisionGroupId) {
    return getCohortLatestVersionOrThrow(studyId, cohortRevisionGroupId);
  }

  /**
   * Fetch the specified version of this cohort. Used for cohort reviews, so transaction annotation
   * lives on the ReviewDao methods.
   */
  public Cohort getCohortVersionOrThrow(String studyId, String cohortId) {
    return getCohortIfExistsHelper(studyId, false, cohortId, null)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Cohort %s, %s not found.", studyId, cohortId)));
  }

  /**
   * Fetch the most recent version for the given revision group id. Used internally so this method
   * has no transaction annotation.
   */
  public Cohort getCohortLatestVersionOrThrow(String studyId, String cohortRevisionGroupId) {
    return getCohortIfExistsHelper(studyId, true, null, cohortRevisionGroupId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Cohort %s, %s not found.", studyId, cohortRevisionGroupId)));
  }

  /** Helper method for fetching a single cohort. */
  @SuppressWarnings("PMD.InsufficientStringBufferDeclaration")
  private Optional<Cohort> getCohortIfExistsHelper(
      String studyId,
      boolean getByRevisionGroupId,
      @Nullable String cohortId,
      @Nullable String cohortRevisionGroupId) {
    if (studyId == null
        || (!getByRevisionGroupId && cohortId == null)
        || (getByRevisionGroupId && cohortRevisionGroupId == null)) {
      throw new MissingRequiredFieldException("Valid study and cohort ids are required");
    }

    StringBuilder sql = new StringBuilder(COHORT_SELECT_SQL + " WHERE study_id = :study_id ");
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("study_id", studyId);
    if (getByRevisionGroupId) {
      sql.append(" AND cohort_revision_group_id = :cohort_revision_group_id AND is_most_recent");
      params.addValue("cohort_revision_group_id", cohortRevisionGroupId);
    } else {
      sql.append(" AND cohort_id = :cohort_id");
      params.addValue("cohort_id", cohortId);
    }

    Cohort.Builder result;
    try {
      result =
          DataAccessUtils.requiredSingleResult(
              jdbcTemplate.query(sql.toString(), params, COHORT_ROW_MAPPER));
      LOGGER.info("Retrieved cohort record {}", result);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
    populateCriteriaGroups(List.of(result));
    return Optional.of(result.build());
  }

  /** Helper method for reading in criteria groups and populating the builder object. */
  private void populateCriteriaGroups(List<Cohort.Builder> cohorts) {
    if (cohorts.isEmpty()) {
      return;
    }

    // Read the criteria groups.
    final String criteriaGroupSql =
        CRITERIA_GROUP_SELECT_SQL + " WHERE cohort_id IN (:cohort_ids) ORDER BY display_name";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(
                "cohort_ids",
                cohorts.stream().map(Cohort.Builder::getCohortId).collect(Collectors.toList()));
    List<CriteriaGroup.Builder> criteriaGroups =
        jdbcTemplate.query(criteriaGroupSql, params, CRITERIA_GROUP_ROW_MAPPER);
    if (criteriaGroups.isEmpty()) {
      return;
    }

    // Read the criterias.
    final String criteriaSql =
        CRITERIA_SELECT_SQL
            + " WHERE criteria_group_id IN (:criteria_group_ids) ORDER BY display_name";
    params =
        new MapSqlParameterSource()
            .addValue(
                "criteria_group_ids",
                criteriaGroups.stream()
                    .map(CriteriaGroup.Builder::getCriteriaGroupId)
                    .collect(Collectors.toList()));
    List<Criteria> criterias = jdbcTemplate.query(criteriaSql, params, CRITERIA_ROW_MAPPER);

    // Populate the criteria lists in the criteria groups.
    criterias.stream()
        .forEach(
            criteria ->
                criteriaGroups.stream()
                    .filter(
                        criteriaGroup ->
                            criteriaGroup
                                .getCriteriaGroupId()
                                .equals(criteria.getCriteriaGroupId()))
                    .findFirst()
                    .get()
                    .addCriteria(criteria));

    // Populate the criteria group lists in the cohorts.
    criteriaGroups.stream()
        .forEach(
            criteriaGroupBuilder -> {
              CriteriaGroup criteriaGroup = criteriaGroupBuilder.build();
              cohorts.stream()
                  .filter(cohort -> cohort.getCohortId().equals(criteriaGroup.getCohortId()))
                  .findFirst()
                  .get()
                  .addCriteriaGroup(criteriaGroup);
            });
  }

  /** Create a new cohort that is the first version in this revision group. */
  @WriteTransaction
  public void createCohortFirstVersion(Cohort cohort) {
    Optional<Cohort> existingCohort =
        getCohortIfExistsHelper(cohort.getStudyId(), true, null, cohort.getCohortRevisionGroupId());
    if (existingCohort.isPresent()) {
      throw new DuplicateCohortException(
          String.format(
              "First version of cohort in revision group %s already exists - display name %s",
              cohort.getCohortRevisionGroupId(), cohort.getDisplayName()));
    }
    createCohortHelper(cohort);
  }

  /**
   * Update the latest version of a cohort. If it's editable, then modify the fields directly. If
   * it's frozen, then create a new version with the updated properties, and set that one as the
   * most recent.
   */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  @WriteTransaction
  public boolean updateCohortLatestVersion(
      String studyId,
      String cohortRevisionGroupId,
      @Nullable String displayName,
      @Nullable String description,
      @Nullable List<CriteriaGroup> criteriaGroups) {
    Cohort existingCohort = getCohortLatestVersionOrThrow(studyId, cohortRevisionGroupId);
    if (existingCohort.isEditable()) {
      return updateCohortHelper(
          studyId, existingCohort.getCohortId(), displayName, description, criteriaGroups);
    } else {
      String sql =
          "UPDATE cohort SET is_most_recent = FALSE, last_modified = :last_modified WHERE study_id = :study_id AND cohort_id = :cohort_id";
      MapSqlParameterSource params =
          new MapSqlParameterSource()
              .addValue("study_id", studyId)
              .addValue("cohort_id", existingCohort.getCohortId())
              .addValue("last_modified", Timestamp.from(Instant.now()));
      int rowsAffected = jdbcTemplate.update(sql, params);
      boolean updated = rowsAffected > 0;
      LOGGER.info(
          "{} record for cohort {}, {}",
          updated ? "Updated" : "No Update - did not find",
          studyId,
          cohortRevisionGroupId);

      Cohort.Builder newCohortVersion = existingCohort.toBuilder();
      if (displayName != null) {
        newCohortVersion.displayName(displayName);
      }
      if (description != null) {
        newCohortVersion.description(description);
      }
      if (criteriaGroups != null) {
        newCohortVersion.criteriaGroups(criteriaGroups);
      }

      createCohortHelper(newCohortVersion.build());
      return true;
    }
  }

  /** Helper method for creating a new cohort version. */
  private void createCohortHelper(Cohort cohort) {
    // Store the cohort. New cohort rows are always the most recent and editable.
    final String cohortSql =
        "INSERT INTO cohort (study_id, cohort_id, underlay_name, cohort_revision_group_id, version, is_most_recent, is_editable, last_modified, display_name, description) "
            + "VALUES (:study_id, :cohort_id, :underlay_name, :cohort_revision_group_id, :version, TRUE, TRUE, :last_modified, :display_name, :description)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", cohort.getStudyId())
            .addValue("cohort_id", cohort.getCohortId())
            .addValue("underlay_name", cohort.getUnderlayName())
            .addValue("cohort_revision_group_id", cohort.getCohortRevisionGroupId())
            .addValue("version", cohort.getVersion())
            .addValue("last_modified", Timestamp.from(Instant.now()))
            .addValue("display_name", cohort.getDisplayName())
            .addValue("description", cohort.getDescription());
    try {
      jdbcTemplate.update(cohortSql, params);
      LOGGER.info("Inserted record for cohort {}", cohort.getCohortId());
    } catch (DuplicateKeyException dkEx) {
      if (dkEx.getMessage()
          .contains("duplicate key value violates unique constraint \"cohort_pkey\"")) {
        throw new DuplicateStudyException(
            String.format(
                "Cohort with id %s already exists - display name %s",
                cohort.getCohortId(), cohort.getDisplayName()),
            dkEx);
      } else {
        throw dkEx;
      }
    }

    // Store the criteria groups and criteria.
    insertCriteriaGroups(cohort.getStudyId(), cohort.getCohortId(), cohort.getCriteriaGroups());
  }

  /** Helper method for inserting criteria groups into the auxiliary table. */
  private boolean insertCriteriaGroups(
      String studyId, String cohortId, List<CriteriaGroup> criteriaGroups) {
    // Store the criteria groups.
    final String criteriaGroupSql =
        "INSERT INTO criteria_group (cohort_id, criteria_group_id, user_facing_criteria_group_id, display_name, operator, is_excluded) "
            + "VALUES (:cohort_id, :criteria_group_id, :user_facing_criteria_group_id, :display_name, :operator, :is_excluded)";
    List<MapSqlParameterSource> paramSets =
        criteriaGroups.stream()
            .map(
                criteriaGroup ->
                    new MapSqlParameterSource()
                        .addValue("cohort_id", cohortId)
                        .addValue("criteria_group_id", criteriaGroup.getCriteriaGroupId())
                        .addValue(
                            "user_facing_criteria_group_id",
                            criteriaGroup.getUserFacingCriteriaGroupId())
                        .addValue("display_name", criteriaGroup.getDisplayName())
                        .addValue("operator", criteriaGroup.getOperator().name())
                        .addValue("is_excluded", criteriaGroup.isExcluded()))
            .collect(Collectors.toList());
    jdbcTemplate.batchUpdate(criteriaGroupSql, paramSets.toArray(new MapSqlParameterSource[0]));
    LOGGER.info("Inserted criteria group records for cohort {}, {}", studyId, cohortId);

    // Store the criteria.
    final String criteriaSql =
        "INSERT INTO criteria (criteria_group_id, criteria_id, user_facing_criteria_id, display_name, plugin_name, selection_data, ui_config) "
            + "VALUES (:criteria_group_id, :criteria_id, :user_facing_criteria_id, :display_name, :plugin_name, :selection_data, :ui_config)";
    paramSets = new ArrayList<>();
    for (CriteriaGroup criteriaGroup : criteriaGroups) {
      paramSets.addAll(
          criteriaGroup.getCriterias().stream()
              .map(
                  criteria ->
                      new MapSqlParameterSource()
                          .addValue("criteria_group_id", criteria.getCriteriaGroupId())
                          .addValue("criteria_id", criteria.getCriteriaId())
                          .addValue("user_facing_criteria_id", criteria.getUserFacingCriteriaId())
                          .addValue("display_name", criteria.getDisplayName())
                          .addValue("plugin_name", criteria.getPluginName())
                          .addValue("selection_data", criteria.getSelectionData())
                          .addValue("ui_config", criteria.getUiConfig()))
              .collect(Collectors.toList()));
    }
    int[] numRows =
        jdbcTemplate.batchUpdate(criteriaSql, paramSets.toArray(new MapSqlParameterSource[0]));
    LOGGER.info("Inserted criteria records for cohort {}, {}", studyId, cohortId);
    return Arrays.stream(numRows).allMatch(updatedRow -> updatedRow == 1);
  }

  /** Helper method for deleting criteria groups from the auxiliary table. */
  private boolean deleteCriteriaGroups(String studyId, String cohortId) {
    // Delete the criteria groups. Criteria rows will cascade delete.
    final String sql = "DELETE FROM criteria_group WHERE cohort_id = :cohort_id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("cohort_id", cohortId);
    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean deleted = rowsAffected > 0;
    if (deleted) {
      LOGGER.info("Deleted criteria group records for cohort {}, {}", studyId, cohortId);
    } else {
      LOGGER.info("No criteria group records found for delete cohort {}, {}", studyId, cohortId);
    }
    return deleted;
  }

  /** Helper method for updating an existing cohort version. */
  private boolean updateCohortHelper(
      String studyId,
      String cohortId,
      @Nullable String name,
      @Nullable String description,
      @Nullable List<CriteriaGroup> criteriaGroups) {
    if (name == null && description == null && criteriaGroups == null) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    // Update cohort.
    boolean updated = false;
    if (name != null || description != null) {
      MapSqlParameterSource params = new MapSqlParameterSource();
      if (name != null) {
        params.addValue("display_name", name);
      }
      if (description != null) {
        params.addValue("description", description);
      }
      String sql =
          String.format(
              "UPDATE cohort SET %s WHERE study_id = :study_id AND cohort_id = :cohort_id AND is_most_recent AND is_editable",
              DbUtils.setColumnsClause(params));
      params.addValue("study_id", studyId).addValue("cohort_id", cohortId);
      int rowsAffected = jdbcTemplate.update(sql, params);
      updated = rowsAffected > 0;
      LOGGER.info(
          "{} record for cohort {}, {}",
          updated ? "Updated" : "No Update - did not find",
          studyId,
          cohortId);
    }

    // Update criteria groups.
    if (criteriaGroups != null) {
      boolean deleted = deleteCriteriaGroups(studyId, cohortId);
      boolean inserted = insertCriteriaGroups(studyId, cohortId, criteriaGroups);
      updated |= deleted || inserted;
    }
    return updated;
  }

  /**
   * Freeze the latest version of a cohort. Sets the most recent version to not editable. Used for
   * cohort reviews, so transaction annotation lives on the ReviewDao methods.
   */
  public void freezeCohortLatestVersionOrThrow(String studyId, String cohortRevisionGroupId) {
    String sql =
        "UPDATE cohort SET is_editable = FALSE, last_modified = :last_modified WHERE study_id = :study_id AND cohort_revision_group_id = :cohort_revision_group_id AND is_most_recent";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("cohort_revision_group_id", cohortRevisionGroupId)
            .addValue("last_modified", Timestamp.from(Instant.now()));
    int rowsAffected = jdbcTemplate.update(sql, params);
    if (rowsAffected > 0) {
      LOGGER.info("Updated record for cohort {}, {}", studyId, cohortRevisionGroupId);
    } else {
      throw new BadRequestException(
          "Freezing cohort latest revision failed: " + studyId + ", " + cohortRevisionGroupId);
    }
  }

  /** Delete a cohort revision group, which includes the current revision and all frozen ones. */
  @WriteTransaction
  public boolean deleteCohortAllVersions(String studyId, String cohortRevisionGroupId) {
    return deleteCohortHelper(studyId, true, null, cohortRevisionGroupId);
  }

  /**
   * Delete a cohort version. Used for cohort reviews, so transaction annotation lives on the
   * ReviewDao methods.
   */
  public boolean deleteCohortVersion(String studyId, String cohortId) {
    return deleteCohortHelper(studyId, false, cohortId, null);
  }

  /** Helper method for deleting a cohort. */
  @SuppressWarnings("PMD.InsufficientStringBufferDeclaration")
  private boolean deleteCohortHelper(
      String studyId,
      boolean deleteByRevisionGroupId,
      @Nullable String cohortId,
      @Nullable String cohortRevisionGroupId) {
    StringBuilder sql = new StringBuilder("DELETE FROM cohort WHERE study_id = :study_id");
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("study_id", studyId);
    if (deleteByRevisionGroupId) {
      sql.append(" AND cohort_revision_group_id = :cohort_revision_group_id");
      params.addValue("cohort_revision_group_id", cohortRevisionGroupId);
    } else {
      sql.append(" AND cohort_id = :cohort_id");
      params.addValue("cohort_id", cohortId);
    }
    int rowsAffected = jdbcTemplate.update(sql.toString(), params);
    boolean deleted = rowsAffected > 0;
    if (deleted) {
      LOGGER.info("Deleted record for cohort {}, {}, {}", studyId, cohortId, cohortRevisionGroupId);
    } else {
      LOGGER.info(
          "No record found for delete cohort {}, {}, {}", studyId, cohortId, cohortRevisionGroupId);
    }
    // Criteria groups and criteria rows will cascade delete.
    return deleted;
  }
}
