package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.artifact.ConceptSet;
import bio.terra.tanagra.service.artifact.Criteria;
import java.util.*;
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
public class ConceptSetDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConceptSetDao.class);

  // SQL query and row mapper for reading a concept set.
  private static final String CONCEPT_SET_SELECT_SQL =
      "SELECT id, underlay, entity, display_name, description, created, created_by, last_modified, last_modified_by FROM concept_set";
  private static final RowMapper<ConceptSet.Builder> CONCEPT_SET_ROW_MAPPER =
      (rs, rowNum) ->
          ConceptSet.builder()
              .id(rs.getString("id"))
              .underlay(rs.getString("underlay"))
              .entity(rs.getString("entity"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .created(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
              .createdBy(rs.getString("created_by"))
              .lastModified(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
              .lastModifiedBy(rs.getString("last_modified_by"));

  // SQL query and row mapper for reading a criteria.
  private static final String CRITERIA_SELECT_SQL =
      "SELECT concept_set_id, id, display_name, plugin_name, selection_data, ui_config FROM criteria";
  private static final RowMapper<Pair<String, Criteria.Builder>> CRITERIA_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              rs.getString("concept_set_id"),
              Criteria.builder()
                  .id(rs.getString("id"))
                  .displayName(rs.getString("display_name"))
                  .pluginName(rs.getString("plugin_name"))
                  .selectionData(rs.getString("selection_data"))
                  .uiConfig(rs.getString("ui_config")));

  // SQL query and row mapper for reading a criteria tag.
  private static final String CRITERIA_TAG_SELECT_SQL =
      "SELECT criteria_id, concept_set_id, criteria_key, criteria_value FROM criteria_tag";
  private static final RowMapper<Pair<List<String>, Pair<String, String>>> CRITERIA_TAG_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              List.of(rs.getString("criteria_id"), rs.getString("concept_set_id")),
              Pair.of(rs.getString("criteria_key"), rs.getString("criteria_value")));
  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public ConceptSetDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @ReadTransaction
  public List<ConceptSet> getAllConceptSets(String studyId, int offset, int limit) {
    String sql =
        CONCEPT_SET_SELECT_SQL
            + " WHERE study_id = :study_id ORDER BY display_name LIMIT :limit OFFSET :offset";
    LOGGER.debug("GET ALL concept sets: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    List<ConceptSet> conceptSets = getConceptSetsHelper(sql, params);
    LOGGER.debug("GET ALL concept sets numFound = {}", conceptSets.size());
    return conceptSets;
  }

  @ReadTransaction
  public List<ConceptSet> getConceptSetsMatchingList(Set<String> ids, int offset, int limit) {
    String sql =
        CONCEPT_SET_SELECT_SQL
            + " WHERE id IN (:ids) ORDER BY display_name LIMIT :limit OFFSET :offset";
    LOGGER.debug("GET MATCHING concept sets: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("ids", ids)
            .addValue("offset", offset)
            .addValue("limit", limit);
    List<ConceptSet> conceptSets = getConceptSetsHelper(sql, params);
    LOGGER.debug("GET MATCHING concept sets numFound = {}", conceptSets.size());
    return conceptSets;
  }

  @ReadTransaction
  public ConceptSet getConceptSet(String id) {
    // Fetch concept set.
    String sql = CONCEPT_SET_SELECT_SQL + " WHERE id = :id";
    LOGGER.debug("GET concept set: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    List<ConceptSet> conceptSets = getConceptSetsHelper(sql, params);
    LOGGER.debug("GET concept set numFound = {}", conceptSets.size());

    // Make sure there's only one concept set returned for this id.
    if (conceptSets.isEmpty()) {
      throw new NotFoundException("Concept set not found " + id);
    } else if (conceptSets.size() > 1) {
      throw new SystemException("Multiple concept sets found " + id);
    }
    return conceptSets.get(0);
  }

  @WriteTransaction
  public void deleteConceptSet(String id) {
    String sql = "DELETE FROM concept_set WHERE id = :id";
    LOGGER.debug("DELETE concept set: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE concept set rowsAffected = {}", rowsAffected);
  }

  @WriteTransaction
  public void createConceptSet(String studyId, ConceptSet conceptSet) {
    // Write the concept set. The created and last_modified fields are set by the DB automatically
    // on insert.
    String sql =
        "INSERT INTO concept_set (study_id, id, underlay, entity, created_by, last_modified_by, display_name, description) "
            + "VALUES (:study_id, :id, :underlay, :entity, :created_by, :last_modified_by, :display_name, :description)";
    LOGGER.debug("CREATE concept set: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("id", conceptSet.getId())
            .addValue("underlay", conceptSet.getUnderlay())
            .addValue("entity", conceptSet.getEntity())
            .addValue("created_by", conceptSet.getCreatedBy())
            .addValue("last_modified_by", conceptSet.getLastModifiedBy())
            .addValue("display_name", conceptSet.getDisplayName())
            .addValue("description", conceptSet.getDescription());
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("CREATE concept set rowsAffected = {}", rowsAffected);

    // Write the criteria.
    updateCriteriaHelper(conceptSet.getId(), conceptSet.getCriteria());
  }

  @WriteTransaction
  public void updateConceptSet(
      String id,
      String lastModifiedBy,
      String displayName,
      String description,
      String entity,
      List<Criteria> criteria) {
    // Update the concept set: display name, description, entity, last modified, last modified by.
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("last_modified", DbUtils.sqlTimestampUTC())
            .addValue("last_modified_by", lastModifiedBy);
    if (displayName != null) {
      params.addValue("display_name", displayName);
    }
    if (description != null) {
      params.addValue("description", description);
    }
    if (entity != null) {
      params.addValue("entity", entity);
    }
    String sql =
        String.format("UPDATE concept_set SET %s WHERE id = :id", DbUtils.setColumnsClause(params));
    params.addValue("id", id);
    LOGGER.debug("UPDATE concept set: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE concept set rowsAffected = {}", rowsAffected);

    // Write the criteria.
    if (criteria != null && !criteria.isEmpty()) {
      updateCriteriaHelper(id, criteria);
    }
  }

  private List<ConceptSet> getConceptSetsHelper(
      String conceptSetsSql, MapSqlParameterSource conceptSetsParams) {
    // Fetch concept sets.
    List<ConceptSet.Builder> conceptSets =
        jdbcTemplate.query(conceptSetsSql, conceptSetsParams, CONCEPT_SET_ROW_MAPPER);
    if (conceptSets.isEmpty()) {
      return Collections.emptyList();
    }

    // Fetch criteria. (concept set id -> criteria)
    String sql = CRITERIA_SELECT_SQL + " WHERE concept_set_id IN (:concept_set_ids)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(
                "concept_set_ids",
                conceptSets.stream().map(cs -> cs.getId()).collect(Collectors.toSet()));
    List<Pair<String, Criteria.Builder>> criterias =
        jdbcTemplate.query(sql, params, CRITERIA_ROW_MAPPER);

    // Fetch criteria tags. (criteria id, concept set id -> tag)
    sql = CRITERIA_TAG_SELECT_SQL + " WHERE concept_set_id IN (:concept_set_ids)";
    List<Pair<List<String>, Pair<String, String>>> tags =
        jdbcTemplate.query(sql, params, CRITERIA_TAG_ROW_MAPPER);

    // Put the tags into their respective criteria.
    Map<List<String>, Criteria.Builder> criteriasMap =
        criterias.stream()
            .collect(
                Collectors.toMap(
                    pair -> List.of(pair.getValue().getId(), pair.getKey()),
                    pair -> pair.getValue()));
    tags.stream()
        .forEach(
            pair -> {
              List<String> criteraAndConceptSetId = pair.getKey();
              String tagKey = pair.getValue().getKey();
              String tagValue = pair.getValue().getValue();
              criteriasMap.get(criteraAndConceptSetId).addTag(tagKey, tagValue);
            });

    // Put criteria into their respective concept sets.
    Map<String, ConceptSet.Builder> conceptSetsMap =
        conceptSets.stream()
            .collect(Collectors.toMap(ConceptSet.Builder::getId, Function.identity()));
    criterias.stream()
        .forEach(
            pair -> {
              String conceptSetId = pair.getKey();
              Criteria criteria = pair.getValue().build();
              conceptSetsMap.get(conceptSetId).addCriteria(criteria);
            });

    return conceptSetsMap.values().stream()
        .map(ConceptSet.Builder::build)
        .collect(Collectors.toList());
  }

  private void updateCriteriaHelper(String conceptSetId, List<Criteria> criteria) {
    // Delete any existing criteria.
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("concept_set_id", conceptSetId);
    String sql = "DELETE FROM criteria WHERE concept_set_id = :concept_set_id";
    LOGGER.debug("DELETE criteria: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE criteria rowsAffected = {}", rowsAffected);

    // Delete any existing criteria tags.
    sql = "DELETE FROM criteria_tag WHERE concept_set_id = :concept_set_id";
    LOGGER.debug("DELETE criteria tag: {}", sql);
    rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE criteria tag rowsAffected = {}", rowsAffected);

    // Write the criteria.
    sql =
        "INSERT INTO criteria (concept_set_id, id, display_name, plugin_name, selection_data, ui_config, list_index) "
            + "VALUES (:concept_set_id, :id, :display_name, :plugin_name, :selection_data, :ui_config, :list_index)";
    LOGGER.debug("CREATE criteria: {}", sql);
    List<MapSqlParameterSource> criteriaParamSets =
        criteria.stream()
            .map(
                c ->
                    new MapSqlParameterSource()
                        .addValue("concept_set_id", conceptSetId)
                        .addValue("id", c.getId())
                        .addValue("display_name", c.getDisplayName())
                        .addValue("plugin_name", c.getPluginName())
                        .addValue("selection_data", c.getSelectionData())
                        .addValue("ui_config", c.getUiConfig())
                        .addValue("list_index", 0))
            .collect(Collectors.toList());
    rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(
                    sql, criteriaParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE criteria rowsAffected = {}", rowsAffected);

    // Write the criteria tags.
    sql =
        "INSERT INTO criteria_tag (concept_set_id, criteria_id, criteria_key, criteria_value) VALUES (:concept_set_id, :criteria_id, :key, :value)";
    LOGGER.debug("CREATE criteria tag: {}", sql);
    List<MapSqlParameterSource> tagParamSets = new ArrayList<>();
    criteria.stream()
        .forEach(
            c ->
                tagParamSets.addAll(
                    c.getTags().entrySet().stream()
                        .map(
                            tag ->
                                new MapSqlParameterSource()
                                    .addValue("concept_set_id", conceptSetId)
                                    .addValue("criteria_id", c.getId())
                                    .addValue("key", tag.getKey())
                                    .addValue("value", tag.getValue()))
                        .collect(Collectors.toList())));
    rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(sql, tagParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE criteria tag rowsAffected = {}", rowsAffected);
  }
}
