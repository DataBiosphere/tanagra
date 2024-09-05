package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.artifact.model.Criteria;
import bio.terra.tanagra.service.artifact.model.FeatureSet;
import java.util.ArrayList;
import java.util.Arrays;
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
public class FeatureSetDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureSetDao.class);

  // SQL query and row mapper for reading a feature set.
  private static final String FEATURE_SET_SELECT_SQL =
      "SELECT id, underlay, display_name, description, created, created_by, last_modified, last_modified_by, is_deleted FROM concept_set";
  private static final RowMapper<FeatureSet.Builder> FEATURE_SET_ROW_MAPPER =
      (rs, rowNum) ->
          FeatureSet.builder()
              .id(rs.getString("id"))
              .underlay(rs.getString("underlay"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .created(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
              .createdBy(rs.getString("created_by"))
              .lastModified(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
              .lastModifiedBy(rs.getString("last_modified_by"))
              .isDeleted(rs.getBoolean("is_deleted"));

  // SQL query and row mapper for reading a criteria.
  private static final String CRITERIA_SELECT_SQL =
      "SELECT concept_set_id, id, display_name, plugin_name, plugin_version, predefined_id, selector_or_modifier_name, selection_data, ui_config FROM criteria";
  private static final RowMapper<Pair<String, Criteria.Builder>> CRITERIA_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              rs.getString("concept_set_id"),
              Criteria.builder()
                  .id(rs.getString("id"))
                  .displayName(rs.getString("display_name"))
                  .pluginName(rs.getString("plugin_name"))
                  .pluginVersion(rs.getInt("plugin_version"))
                  .predefinedId(rs.getString("predefined_id"))
                  .selectorOrModifierName(rs.getString("selector_or_modifier_name"))
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

  // SQL query and row mapper for reading an output attribute.
  private static final String OUTPUT_ATTRIBUTE_SELECT_SQL =
      "SELECT concept_set_id, entity, exclude_attribute FROM output_attribute";
  private static final RowMapper<Pair<String, Pair<String, String>>> OUTPUT_ATTRIBUTE_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              rs.getString("concept_set_id"),
              Pair.of(rs.getString("entity"), rs.getString("exclude_attribute")));
  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public FeatureSetDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @ReadTransaction
  public List<FeatureSet> getAllFeatureSets(String studyId, int offset, int limit) {
    String sql =
        FEATURE_SET_SELECT_SQL
            + " WHERE study_id = :study_id AND NOT is_deleted ORDER BY display_name LIMIT :limit OFFSET :offset";
    LOGGER.debug("GET ALL feature sets: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    List<FeatureSet> featureSets = getFeatureSetsHelper(sql, params);
    LOGGER.debug("GET ALL feature sets numFound = {}", featureSets.size());
    return featureSets;
  }

  @ReadTransaction
  public List<FeatureSet> getFeatureSetsMatchingList(Set<String> ids, int offset, int limit) {
    String sql =
        FEATURE_SET_SELECT_SQL
            + " WHERE id IN (:ids) AND NOT is_deleted ORDER BY display_name LIMIT :limit OFFSET :offset";
    LOGGER.debug("GET MATCHING feature sets: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("ids", ids)
            .addValue("offset", offset)
            .addValue("limit", limit);
    List<FeatureSet> featureSets = getFeatureSetsHelper(sql, params);
    LOGGER.debug("GET MATCHING feature sets numFound = {}", featureSets.size());
    return featureSets;
  }

  @ReadTransaction
  public FeatureSet getFeatureSet(String id) {
    // Fetch feature set.
    String sql = FEATURE_SET_SELECT_SQL + " WHERE id = :id";
    LOGGER.debug("GET feature set: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    List<FeatureSet> featureSets = getFeatureSetsHelper(sql, params);
    LOGGER.debug("GET feature set numFound = {}", featureSets.size());

    // Make sure there's only one feature set returned for this id.
    if (featureSets.isEmpty()) {
      throw new NotFoundException("Feature set not found " + id);
    } else if (featureSets.size() > 1) {
      throw new SystemException("Multiple feature sets found " + id);
    }
    return featureSets.get(0);
  }

  @WriteTransaction
  public void deleteFeatureSet(String id) {
    String sql = "UPDATE concept_set SET is_deleted = true WHERE id = :id";
    LOGGER.debug("DELETE feature set: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE feature set rowsAffected = {}", rowsAffected);
  }

  @WriteTransaction
  public void createFeatureSet(String studyId, FeatureSet featureSet) {
    // Write the feature set. The created and last_modified fields are set by the DB automatically
    // on insert.
    String sql =
        "INSERT INTO concept_set (study_id, id, underlay, created_by, last_modified_by, display_name, description, is_deleted) "
            + "VALUES (:study_id, :id, :underlay, :created_by, :last_modified_by, :display_name, :description, false)";
    LOGGER.debug("CREATE feature set: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("id", featureSet.getId())
            .addValue("underlay", featureSet.getUnderlay())
            .addValue("created_by", featureSet.getCreatedBy())
            .addValue("last_modified_by", featureSet.getLastModifiedBy())
            .addValue("display_name", featureSet.getDisplayName())
            .addValue("description", featureSet.getDescription());
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("CREATE feature set rowsAffected = {}", rowsAffected);

    // Write the criteria.
    if (featureSet.getCriteria() != null && !featureSet.getCriteria().isEmpty()) {
      updateCriteriaHelper(featureSet.getId(), featureSet.getCriteria());
    }

    // Write the output attributes.
    if (featureSet.getExcludeOutputAttributesPerEntity() != null
        && !featureSet.getExcludeOutputAttributesPerEntity().isEmpty()) {
      updateOutputAttributesHelper(
          featureSet.getId(), featureSet.getExcludeOutputAttributesPerEntity());
    }
  }

  @WriteTransaction
  public void updateFeatureSet(
      String id,
      String lastModifiedBy,
      String displayName,
      String description,
      List<Criteria> criteria,
      Map<String, List<String>> outputAttributesPerEntity) {
    if (displayName == null
        && description == null
        && (criteria == null)
        && (outputAttributesPerEntity == null || outputAttributesPerEntity.isEmpty())) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    // Check to make sure the feature set isn't deleted.
    FeatureSet featureSet = getFeatureSet(id);
    if (featureSet.isDeleted()) {
      throw new NotFoundException("Feature set " + id + " has been deleted.");
    }

    // Update the feature set: display name, description, last modified, last modified by.
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("last_modified", JdbcUtils.sqlTimestampUTC())
            .addValue("last_modified_by", lastModifiedBy);
    if (displayName != null) {
      params.addValue("display_name", displayName);
    }
    if (description != null) {
      params.addValue("description", description);
    }
    String sql =
        String.format(
            "UPDATE concept_set SET %s WHERE id = :id", JdbcUtils.setColumnsClause(params));
    params.addValue("id", id);
    LOGGER.debug("UPDATE feature set: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE feature set rowsAffected = {}", rowsAffected);

    // Write the criteria.
    if (criteria != null) {
      updateCriteriaHelper(id, criteria);
    }

    // Write the output attributes.
    if (outputAttributesPerEntity != null && !outputAttributesPerEntity.isEmpty()) {
      updateOutputAttributesHelper(id, outputAttributesPerEntity);
    }
  }

  private List<FeatureSet> getFeatureSetsHelper(
      String featureSetsSql, MapSqlParameterSource featureSetsParams) {
    // Fetch feature sets.
    List<FeatureSet.Builder> featureSets =
        jdbcTemplate.query(featureSetsSql, featureSetsParams, FEATURE_SET_ROW_MAPPER);
    if (featureSets.isEmpty()) {
      return Collections.emptyList();
    }

    // Fetch criteria. (feature set id -> criteria)
    String sql = CRITERIA_SELECT_SQL + " WHERE concept_set_id IN (:concept_set_ids)";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(
                "concept_set_ids",
                featureSets.stream().map(FeatureSet.Builder::getId).collect(Collectors.toSet()));
    List<Pair<String, Criteria.Builder>> criterias =
        jdbcTemplate.query(sql, params, CRITERIA_ROW_MAPPER);

    // Fetch criteria tags. ([criteria id, feature set id] -> tag key-value pair)
    sql = CRITERIA_TAG_SELECT_SQL + " WHERE concept_set_id IN (:concept_set_ids)";
    List<Pair<List<String>, Pair<String, String>>> tags =
        jdbcTemplate.query(sql, params, CRITERIA_TAG_ROW_MAPPER);

    // Fetch output attributes. (feature set id -> output entity-attribute pair)
    sql = OUTPUT_ATTRIBUTE_SELECT_SQL + " WHERE concept_set_id IN (:concept_set_ids)";
    List<Pair<String, Pair<String, String>>> outputAttributes =
        jdbcTemplate.query(sql, params, OUTPUT_ATTRIBUTE_ROW_MAPPER);

    // Put the tags into their respective criteria.
    Map<List<String>, Criteria.Builder> criteriasMap =
        criterias.stream()
            .collect(
                Collectors.toMap(
                    pair -> List.of(pair.getValue().getId(), pair.getKey()), Pair::getValue));
    tags.forEach(
        pair -> {
          List<String> criteraAndFeatureSetId = pair.getKey();
          String tagKey = pair.getValue().getKey();
          String tagValue = pair.getValue().getValue();
          criteriasMap.get(criteraAndFeatureSetId).addTag(tagKey, tagValue);
        });

    // Put criteria into their respective feature sets.
    Map<String, FeatureSet.Builder> featureSetsMap =
        featureSets.stream()
            .collect(Collectors.toMap(FeatureSet.Builder::getId, Function.identity()));
    criterias.forEach(
        pair -> {
          String featureSetId = pair.getKey();
          Criteria criteria = pair.getValue().build();
          featureSetsMap.get(featureSetId).addCriteria(criteria);
        });

    // Put the output attributes into their respective feature sets.
    outputAttributes.forEach(
        pair -> {
          String featureSetId = pair.getKey();
          String entity = pair.getValue().getKey();
          String attribute = pair.getValue().getValue();
          featureSetsMap.get(featureSetId).addExcludeOutputAttribute(entity, attribute);
        });

    // Preserve the order returned by the original query.
    return featureSets.stream()
        .map(c -> featureSetsMap.get(c.getId()).build())
        .collect(Collectors.toList());
  }

  private void updateCriteriaHelper(String featureSetId, List<Criteria> criteria) {
    // Delete any existing criteria.
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("concept_set_id", featureSetId);
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
        "INSERT INTO criteria (concept_set_id, id, display_name, plugin_name, plugin_version, predefined_id, selector_or_modifier_name, selection_data, ui_config, list_index) "
            + "VALUES (:concept_set_id, :id, :display_name, :plugin_name, :plugin_version, :predefined_id, :selector_or_modifier_name, :selection_data, :ui_config, :list_index)";
    LOGGER.debug("CREATE criteria: {}", sql);
    MapSqlParameterSource[] criteriaParamSets =
        criteria.stream()
            .map(
                c ->
                    new MapSqlParameterSource()
                        .addValue("concept_set_id", featureSetId)
                        .addValue("id", c.getId())
                        .addValue("display_name", c.getDisplayName())
                        .addValue("plugin_name", c.getPluginName())
                        .addValue("plugin_version", c.getPluginVersion())
                        .addValue("predefined_id", c.getPredefinedId())
                        .addValue("selector_or_modifier_name", c.getSelectorOrModifierName())
                        .addValue("selection_data", c.getSelectionData())
                        .addValue("ui_config", c.getUiConfig())
                        .addValue("list_index", 0))
            .toList()
            .toArray(new MapSqlParameterSource[0]);
    rowsAffected = Arrays.stream(jdbcTemplate.batchUpdate(sql, criteriaParamSets)).sum();
    LOGGER.debug("CREATE criteria rowsAffected = {}", rowsAffected);

    // Write the criteria tags.
    sql =
        "INSERT INTO criteria_tag (concept_set_id, criteria_id, criteria_key, criteria_value) VALUES (:concept_set_id, :criteria_id, :key, :value)";
    LOGGER.debug("CREATE criteria tag: {}", sql);
    List<MapSqlParameterSource> tagParamSets = new ArrayList<>();
    criteria.forEach(
        c ->
            tagParamSets.addAll(
                c.getTags().entrySet().stream()
                    .map(
                        tag ->
                            new MapSqlParameterSource()
                                .addValue("concept_set_id", featureSetId)
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

  private void updateOutputAttributesHelper(
      String featureSetId, Map<String, List<String>> outputAttributes) {
    // Delete any existing output attributes.
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("concept_set_id", featureSetId);
    String sql = "DELETE FROM output_attribute WHERE concept_set_id = :concept_set_id";
    LOGGER.debug("DELETE output attributes: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE output attributes rowsAffected = {}", rowsAffected);

    // Write the output attributes.
    sql =
        "INSERT INTO output_attribute (concept_set_id, entity, exclude_attribute) "
            + "VALUES (:concept_set_id, :entity, :exclude_attribute)";
    LOGGER.debug("CREATE output attribute: {}", sql);
    List<MapSqlParameterSource> outputAttributeParamSets = new ArrayList<>();
    outputAttributes.forEach(
        (entity, value) ->
            value.forEach(
                attribute ->
                    outputAttributeParamSets.add(
                        new MapSqlParameterSource()
                            .addValue("concept_set_id", featureSetId)
                            .addValue("entity", entity)
                            .addValue("exclude_attribute", attribute))));
    rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(
                    sql, outputAttributeParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE output attributes rowsAffected = {}", rowsAffected);
  }
}
