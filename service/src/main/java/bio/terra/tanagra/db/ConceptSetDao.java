package bio.terra.tanagra.db;

import static bio.terra.tanagra.db.CohortDao.CRITERIA_ROW_MAPPER;
import static bio.terra.tanagra.db.CohortDao.CRITERIA_SELECT_SQL;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.db.exception.DuplicateConceptSetException;
import bio.terra.tanagra.service.artifact.ConceptSet;
import bio.terra.tanagra.service.artifact.Criteria;
import java.sql.Timestamp;
import java.time.Instant;
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
public class ConceptSetDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConceptSetDao.class);

  // SQL query and row mapper for reading a concept set.
  private static final String CONCEPT_SET_SELECT_SQL =
      "SELECT study_id, concept_set_id, underlay_name, entity_name, last_modified, display_name, description FROM concept_set";
  private static final RowMapper<ConceptSet.Builder> CONCEPT_SET_ROW_MAPPER =
      (rs, rowNum) ->
          ConceptSet.builder()
              .studyId(rs.getString("study_id"))
              .conceptSetId(rs.getString("concept_set_id"))
              .underlayName(rs.getString("underlay_name"))
              .entityName(rs.getString("entity_name"))
              .lastModified(rs.getTimestamp("last_modified"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"));

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public ConceptSetDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Fetch all concept sets. */
  @ReadTransaction
  public List<ConceptSet> getAllConceptSets(String studyId, int offset, int limit) {
    return getConceptSetsHelper(studyId, offset, limit, null);
  }

  /** Fetch the concept sets. Only returns cohorts with the specified revision group ids. */
  @ReadTransaction
  public List<ConceptSet> getConceptSetsMatchingList(
      String studyId, Set<String> conceptSetIdList, int offset, int limit) {
    return getConceptSetsHelper(studyId, offset, limit, conceptSetIdList);
  }

  /** Helper method for fetching a list of concept sets. */
  @SuppressWarnings("PMD.InsufficientStringBufferDeclaration")
  private List<ConceptSet> getConceptSetsHelper(
      String studyId, int offset, int limit, @Nullable Set<String> conceptSetIdList) {
    if (conceptSetIdList != null && conceptSetIdList.isEmpty()) {
      // If the incoming list is empty, the caller does not have permission to see any concept sets,
      // so we return an empty list.
      return Collections.emptyList();
    }

    StringBuilder sql = new StringBuilder(CONCEPT_SET_SELECT_SQL + " WHERE study_id = :study_id");
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    if (conceptSetIdList != null) {
      sql.append(" AND concept_set_id IN (:concept_set_ids)");
      params.addValue("concept_set_ids", conceptSetIdList);
    }
    sql.append(" ORDER BY display_name OFFSET :offset LIMIT :limit");
    List<ConceptSet.Builder> conceptSets =
        jdbcTemplate.query(sql.toString(), params, CONCEPT_SET_ROW_MAPPER);
    populateCriteria(conceptSets);
    return conceptSets.stream().map(ConceptSet.Builder::build).collect(Collectors.toList());
  }

  /** Fetch the concept set with the given id. */
  @ReadTransaction
  public ConceptSet getConceptSetOrThrow(String studyId, String conceptSetId) {
    return getConceptSetIfExistsHelper(studyId, conceptSetId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Concept set %s, %s not found.", studyId, conceptSetId)));
  }

  /** Helper method for fetching a single concept set. */
  @SuppressWarnings("PMD.InsufficientStringBufferDeclaration")
  private Optional<ConceptSet> getConceptSetIfExistsHelper(
      String studyId, @Nullable String conceptSetId) {
    if (studyId == null || conceptSetId == null) {
      throw new MissingRequiredFieldException("Valid study and concept set ids are required");
    }

    StringBuilder sql =
        new StringBuilder(
            CONCEPT_SET_SELECT_SQL
                + " WHERE study_id = :study_id  AND concept_set_id = :concept_set_id");
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("concept_set_id", conceptSetId);

    ConceptSet.Builder result;
    try {
      result =
          DataAccessUtils.requiredSingleResult(
              jdbcTemplate.query(sql.toString(), params, CONCEPT_SET_ROW_MAPPER));
      LOGGER.info("Retrieved concept set record {}", result);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
    populateCriteria(List.of(result));
    return Optional.of(result.build());
  }

  /** Helper method for reading in the criteria and populating the builder object. */
  private void populateCriteria(List<ConceptSet.Builder> conceptSets) {
    if (conceptSets.isEmpty()) {
      return;
    }

    // Read the criterias.
    final String criteriaSql =
        CRITERIA_SELECT_SQL + " WHERE concept_set_id IN (:concept_set_ids) ORDER BY display_name";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(
                "concept_set_ids",
                conceptSets.stream()
                    .map(ConceptSet.Builder::getConceptSetId)
                    .collect(Collectors.toList()));
    List<Criteria> criterias = jdbcTemplate.query(criteriaSql, params, CRITERIA_ROW_MAPPER);

    // Populate the criteria in the concept set builders.
    criterias.stream()
        .forEach(
            criteria ->
                conceptSets.stream()
                    .filter(
                        conceptSet ->
                            conceptSet.getConceptSetId().equals(criteria.getConceptSetId()))
                    .findFirst()
                    .get()
                    .criteria(criteria));
  }

  /** Create a new concept set. */
  @WriteTransaction
  public void createConceptSet(ConceptSet conceptSet) {
    // Store the cohort. New cohort rows are always the most recent and editable.
    final String sql =
        "INSERT INTO concept_set (study_id, concept_set_id, underlay_name, entity_name, last_modified, display_name, description) "
            + "VALUES (:study_id, :concept_set_id, :underlay_name, :entity_name, :last_modified, :display_name, :description)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", conceptSet.getStudyId())
            .addValue("concept_set_id", conceptSet.getConceptSetId())
            .addValue("underlay_name", conceptSet.getUnderlayName())
            .addValue("entity_name", conceptSet.getEntityName())
            .addValue("last_modified", Timestamp.from(Instant.now()))
            .addValue("display_name", conceptSet.getDisplayName())
            .addValue("description", conceptSet.getDescription());
    try {
      jdbcTemplate.update(sql, params);
      LOGGER.info("Inserted record for concept set {}", conceptSet.getConceptSetId());
    } catch (DuplicateKeyException dkEx) {
      if (dkEx.getMessage()
          .contains("duplicate key value violates unique constraint \"concept_set_pkey\"")) {
        throw new DuplicateConceptSetException(
            String.format(
                "Concept set with id %s already exists - display name %s",
                conceptSet.getConceptSetId(), conceptSet.getDisplayName()),
            dkEx);
      } else {
        throw dkEx;
      }
    }

    // Store the criteria.
    if (conceptSet.getCriteria() != null) {
      insertCriteria(
          conceptSet.getStudyId(), conceptSet.getConceptSetId(), conceptSet.getCriteria());
    }
  }

  /**
   * Update the latest version of a concept set. Currently, the display name, description, entity
   * name, and criteria are modifiable.
   */
  @SuppressWarnings("PMD.UseObjectForClearerAPI")
  @WriteTransaction
  public boolean updateConceptSet(
      String studyId,
      String conceptSetId,
      @Nullable String entityName,
      @Nullable String displayName,
      @Nullable String description,
      @Nullable Criteria criteria) {
    if (displayName == null && description == null && criteria == null) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    // Update concept set.
    boolean updated = false;
    if (displayName != null || description != null) {
      MapSqlParameterSource params = new MapSqlParameterSource();
      if (entityName != null) {
        params.addValue("entity_name", entityName);
      }
      if (displayName != null) {
        params.addValue("display_name", displayName);
      }
      if (description != null) {
        params.addValue("description", description);
      }
      String sql =
          String.format(
              "UPDATE concept_set SET %s WHERE study_id = :study_id AND concept_set_id = :concept_set_id",
              DbUtils.setColumnsClause(params));
      params.addValue("study_id", studyId).addValue("concept_set_id", conceptSetId);
      int rowsAffected = jdbcTemplate.update(sql, params);
      updated = rowsAffected > 0;
      LOGGER.info(
          "{} record for concept set {}, {}",
          updated ? "Updated" : "No Update - did not find",
          studyId,
          conceptSetId);
    }

    // Update criteria.
    if (criteria != null) {
      boolean deleted = deleteCriteria(studyId, conceptSetId);
      boolean inserted = insertCriteria(studyId, conceptSetId, criteria);
      updated |= deleted || inserted;
    }
    return updated;
  }

  /** Helper method for inserting criteria into the auxiliary table. */
  private boolean insertCriteria(String studyId, String conceptSetId, Criteria criteria) {
    // Store the criteria.
    final String sql =
        "INSERT INTO criteria (concept_set_id, criteria_id, user_facing_criteria_id, display_name, plugin_name, selection_data, ui_config) "
            + "VALUES (:concept_set_id, :criteria_id, :user_facing_criteria_id, :display_name, :plugin_name, :selection_data, :ui_config)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("concept_set_id", criteria.getConceptSetId())
            .addValue("criteria_id", criteria.getCriteriaId())
            .addValue("user_facing_criteria_id", criteria.getUserFacingCriteriaId())
            .addValue("display_name", criteria.getDisplayName())
            .addValue("plugin_name", criteria.getPluginName())
            .addValue("selection_data", criteria.getSelectionData())
            .addValue("ui_config", criteria.getUiConfig());
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.info("Inserted criteria records for concept set {}, {}", studyId, conceptSetId);
    return rowsAffected > 0;
  }

  /** Helper method for deleting criteria from the auxiliary table. */
  private boolean deleteCriteria(String studyId, String conceptSetId) {
    // Delete the criteria.
    final String sql = "DELETE FROM criteria WHERE concept_set_id = :concept_set_id";
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("concept_set_id", conceptSetId);
    int rowsAffected = jdbcTemplate.update(sql, params);
    boolean deleted = rowsAffected > 0;
    if (deleted) {
      LOGGER.info("Deleted criteria group records for concept set {}, {}", studyId, conceptSetId);
    } else {
      LOGGER.info(
          "No criteria group records found for delete concept set {}, {}", studyId, conceptSetId);
    }
    return deleted;
  }

  /** Delete a concept set. */
  @WriteTransaction
  public boolean deleteConceptSet(String studyId, String conceptSetId) {
    StringBuilder sql =
        new StringBuilder(
            "DELETE FROM concept_set WHERE study_id = :study_id AND concept_set_id = :concept_set_id");
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("concept_set_id", conceptSetId);
    int rowsAffected = jdbcTemplate.update(sql.toString(), params);
    boolean deleted = rowsAffected > 0;
    if (deleted) {
      LOGGER.info("Deleted record for concept set {}, {}", studyId, conceptSetId);
    } else {
      LOGGER.info("No record found for delete concept set {}, {}", studyId, conceptSetId);
    }
    // Criteria rows will cascade delete.
    return deleted;
  }
}
