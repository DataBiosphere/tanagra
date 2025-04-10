package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.MissingRequiredFieldException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.api.filter.BooleanAndOrFilter;
import bio.terra.tanagra.api.shared.JoinOperator;
import bio.terra.tanagra.api.shared.ReducingOperator;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.Criteria;
import java.util.ArrayList;
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
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class CohortDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(CohortDao.class);

  // SQL query and row mapper for reading a cohort.
  private static final String COHORT_SELECT_SQL =
      "SELECT id, underlay, study_id, created, created_by, last_modified, last_modified_by, display_name, description, is_deleted FROM cohort";
  private static final RowMapper<Cohort.Builder> COHORT_ROW_MAPPER =
      (rs, rowNum) ->
          Cohort.builder()
              .id(rs.getString("id"))
              .underlay(rs.getString("underlay"))
              .studyId(rs.getString("study_id"))
              .created(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
              .createdBy(rs.getString("created_by"))
              .lastModified(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
              .lastModifiedBy(rs.getString("last_modified_by"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"))
              .isDeleted(rs.getBoolean("is_deleted"));

  // SQL query and row mapper for reading a cohort revision.
  private static final String COHORT_REVISION_SELECT_SQL =
      "SELECT cohort_id, id, version, is_most_recent, is_editable, created, created_by, last_modified, last_modified_by, records_count FROM cohort_revision";
  private static final RowMapper<Pair<String, CohortRevision.Builder>> COHORT_REVISION_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              rs.getString("cohort_id"),
              CohortRevision.builder()
                  .id(rs.getString("id"))
                  .version(rs.getInt("version"))
                  .setIsMostRecent(rs.getBoolean("is_most_recent"))
                  .setIsEditable(rs.getBoolean("is_editable"))
                  .created(JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
                  .createdBy(rs.getString("created_by"))
                  .lastModified(
                      JdbcUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
                  .lastModifiedBy(rs.getString("last_modified_by"))
                  .recordsCount(rs.getObject("records_count", Long.class)));

  // SQL query and row mapper for reading a criteria group section.
  private static final String CRITERIA_GROUP_SECTION_SELECT_SQL =
      "SELECT cohort_revision_id, id, display_name, operator, is_excluded, is_disabled, first_condition_reducing_operator, second_condition_reducing_operator, join_operator, join_operator_value FROM criteria_group_section";
  private static final RowMapper<Pair<String, CohortRevision.CriteriaGroupSection.Builder>>
      CRITERIA_GROUP_SECTION_ROW_MAPPER =
          (rs, rowNum) ->
              Pair.of(
                  rs.getString("cohort_revision_id"),
                  CohortRevision.CriteriaGroupSection.builder()
                      .id(rs.getString("id"))
                      .displayName(rs.getString("display_name"))
                      .operator(
                          BooleanAndOrFilter.LogicalOperator.valueOf(rs.getString("operator")))
                      .setIsExcluded(rs.getBoolean("is_excluded"))
                      .setIsDisabled(rs.getBoolean("is_disabled"))
                      .firstConditionReducingOperator(
                          rs.getString("first_condition_reducing_operator") == null
                              ? null
                              : ReducingOperator.valueOf(
                                  rs.getString("first_condition_reducing_operator")))
                      .secondConditionReducingOperator(
                          rs.getString("second_condition_reducing_operator") == null
                              ? null
                              : ReducingOperator.valueOf(
                                  rs.getString("second_condition_reducing_operator")))
                      .joinOperator(
                          rs.getString("join_operator") == null
                              ? null
                              : JoinOperator.valueOf(rs.getString("join_operator")))
                      .joinOperatorValue(rs.getObject("join_operator_value", Integer.class)));

  // SQL query and row mapper for reading a criteria group.
  private static final String CRITERIA_GROUP_SELECT_SQL =
      "SELECT cohort_revision_id, criteria_group_section_id, id, display_name, condition_index, is_disabled FROM criteria_group";
  private static final RowMapper<
          Pair<Pair<List<String>, Integer>, CohortRevision.CriteriaGroup.Builder>>
      CRITERIA_GROUP_ROW_MAPPER =
          (rs, rowNum) ->
              Pair.of(
                  Pair.of(
                      List.of(
                          rs.getString("criteria_group_section_id"),
                          rs.getString("cohort_revision_id")),
                      rs.getObject("condition_index", Integer.class)),
                  CohortRevision.CriteriaGroup.builder()
                      .id(rs.getString("id"))
                      .displayName(rs.getString("display_name"))
                      .isDisabled(rs.getBoolean("is_disabled")));

  // SQL query and row mapper for reading a criteria.
  private static final String CRITERIA_SELECT_SQL =
      "SELECT cohort_revision_id, criteria_group_section_id, criteria_group_id, id, display_name, plugin_name, plugin_version, selector_or_modifier_name, selection_data, ui_config FROM criteria";
  private static final RowMapper<Pair<List<String>, Criteria.Builder>> CRITERIA_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              List.of(
                  rs.getString("criteria_group_id"),
                  rs.getString("criteria_group_section_id"),
                  rs.getString("cohort_revision_id")),
              Criteria.builder()
                  .id(rs.getString("id"))
                  .displayName(rs.getString("display_name"))
                  .pluginName(rs.getString("plugin_name"))
                  .pluginVersion(rs.getInt("plugin_version"))
                  .selectorOrModifierName(rs.getString("selector_or_modifier_name"))
                  .selectionData(rs.getString("selection_data"))
                  .uiConfig(rs.getString("ui_config")));

  // SQL query and row mapper for reading a criteria tag.
  private static final String CRITERIA_TAG_SELECT_SQL =
      "SELECT cohort_revision_id, criteria_group_section_id, criteria_group_id, criteria_id, criteria_key, criteria_value FROM criteria_tag";
  private static final RowMapper<Pair<List<String>, Pair<String, String>>> CRITERIA_TAG_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              List.of(
                  rs.getString("criteria_id"),
                  rs.getString("criteria_group_id"),
                  rs.getString("criteria_group_section_id"),
                  rs.getString("cohort_revision_id")),
              Pair.of(rs.getString("criteria_key"), rs.getString("criteria_value")));
  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public CohortDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @ReadTransaction
  public List<Cohort> getAllCohorts(String studyId, int offset, int limit) {
    String sql =
        COHORT_SELECT_SQL
            + " WHERE study_id = :study_id AND NOT is_deleted ORDER BY display_name LIMIT :limit OFFSET :offset";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return getCohortsHelper(sql, params);
  }

  @ReadTransaction
  public List<Cohort> getCohortsMatchingList(Set<String> ids, int offset, int limit) {
    String sql =
        COHORT_SELECT_SQL
            + " WHERE id IN (:ids) AND NOT is_deleted ORDER BY display_name LIMIT :limit OFFSET :offset";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("ids", ids)
            .addValue("offset", offset)
            .addValue("limit", limit);
    return getCohortsHelper(sql, params);
  }

  @ReadTransaction
  public Cohort getCohort(String id) {
    // Fetch cohort.
    String sql = COHORT_SELECT_SQL + " WHERE id = :id";
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    List<Cohort> cohorts = getCohortsHelper(sql, params);

    // Make sure there's only one cohort returned for this id.
    if (cohorts.isEmpty()) {
      throw new NotFoundException("Cohort not found " + id);
    } else if (cohorts.size() > 1) {
      throw new SystemException("Multiple cohorts found " + id);
    }
    return cohorts.get(0);
  }

  @WriteTransaction
  public void deleteCohort(String id) {
    String sql = "UPDATE cohort SET is_deleted = true WHERE id = :id";
    LOGGER.debug("DELETE cohort: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE cohort rowsAffected = {}", rowsAffected);
  }

  @WriteTransaction
  public void createCohort(String studyId, Cohort cohort) {
    MapSqlParameterSource cohortParamSets =
        buildCohortParam(
            studyId,
            cohort.getId(),
            cohort.getUnderlay(),
            cohort.getCreatedBy(),
            cohort.getDisplayName(),
            cohort.getDescription());
    insertCohortRows(List.of(cohortParamSets));

    // Write the first cohort revision.
    createRevision(cohort.getId(), cohort.getMostRecentRevision());
  }

  @WriteTransaction
  public void updateCohort(
      String id,
      String lastModifiedBy,
      String displayName,
      String description,
      List<CohortRevision.CriteriaGroupSection> criteriaGroupSections) {
    if (displayName == null && description == null && criteriaGroupSections == null) {
      throw new MissingRequiredFieldException("Must specify field to update.");
    }

    // Check to make sure the cohort isn't deleted.
    Cohort cohort = getCohort(id);
    if (cohort.isDeleted()) {
      throw new NotFoundException("Cohort " + id + " has been deleted");
    }

    // Update the cohort: display name, description, last modified, last modified by.
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("last_modified", JdbcUtils.sqlTimestampUTC())
            .addValue("last_modified_by", lastModifiedBy);
    if (displayName != null) {
      params.addValue("display_name", displayName);
    }
    if (description != null) {
      params.addValue("description", description);
    }
    String sql =
        String.format("UPDATE cohort SET %s WHERE id = :id", JdbcUtils.setColumnsClause(params));
    LOGGER.debug("UPDATE cohort: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE cohort rowsAffected = {}", rowsAffected);

    // Update the most recent cohort revision.
    if (!CollectionUtils.isEmpty(criteriaGroupSections)) {
      // Expect a single most recent, editable cohort revision.
      sql =
          "SELECT id FROM cohort_revision WHERE cohort_id = :cohort_id AND is_editable AND is_most_recent";
      params = new MapSqlParameterSource().addValue("cohort_id", id);
      String cohortRevisionId =
          DataAccessUtils.requiredSingleResult(
              jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("id")));

      sql =
          "UPDATE cohort_revision SET last_modified = :last_modified, last_modified_by = :last_modified_by WHERE id = :id";
      LOGGER.debug("UPDATE cohort_revision: {}", sql);
      params =
          new MapSqlParameterSource()
              .addValue("last_modified", JdbcUtils.sqlTimestampUTC())
              .addValue("last_modified_by", lastModifiedBy)
              .addValue("id", cohortRevisionId);
      rowsAffected = jdbcTemplate.update(sql, params);
      LOGGER.debug("UPDATE cohort_revision rowsAffected = {}", rowsAffected);

      // Write the criteria group sections.
      updateCriteriaHelper(cohortRevisionId, criteriaGroupSections);
    }
  }

  /**
   * @return the id of the frozen revision just created
   */
  @WriteTransaction
  public String createNextRevision(
      String cohortId, String reviewId, String userEmail, Long recordsCount) {
    // Get the current most recent revision, so we can copy it.
    Cohort cohort = getCohort(cohortId);
    String frozenRevisionId = cohort.getMostRecentRevision().getId();

    // Update the current revision to be un-editable and no longer the most recent.
    String sql =
        "UPDATE cohort_revision SET review_id = :review_id, is_editable = :is_editable, is_most_recent = :is_most_recent, records_count = :records_count WHERE id = :id";
    LOGGER.debug("UPDATE cohort_revision: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("review_id", reviewId)
            .addValue("is_editable", false)
            .addValue("is_most_recent", false)
            .addValue("records_count", recordsCount)
            .addValue("id", cohort.getMostRecentRevision().getId());
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE cohort_revision rowsAffected = {}", rowsAffected);

    // Create a new revision.
    CohortRevision nextRevision =
        cohort.getMostRecentRevision().toBuilder()
            .setIsEditable(true)
            .setIsMostRecent(true)
            .version(cohort.getMostRecentRevision().getVersion() + 1)
            .createdBy(userEmail)
            .id(null) // Builder will generate a new id.
            .created(null) // Set by DB on insert
            .lastModified(null) // Set by DB on insert
            .recordsCount(null) // Only store the records count for frozen revisions.
            .build();
    createRevision(cohortId, nextRevision);
    return frozenRevisionId;
  }

  private List<Cohort> getCohortsHelper(String cohortsSql, MapSqlParameterSource cohortsParams) {
    // Fetch cohorts.
    List<Cohort.Builder> cohorts = jdbcTemplate.query(cohortsSql, cohortsParams, COHORT_ROW_MAPPER);
    if (cohorts.isEmpty()) {
      return Collections.emptyList();
    }

    // Fetch the most recent cohort revisions. (cohort id -> cohort revision)
    List<Pair<String, CohortRevision.Builder>> cohortRevisions =
        getRevisions(cohorts.stream().map(Cohort.Builder::getId).collect(Collectors.toSet()), true);

    // Populate the criteria.
    getCriteriaHelper(cohortRevisions);

    // Put cohort revisions into their respective cohorts.
    Map<String, Cohort.Builder> cohortsMap =
        cohorts.stream().collect(Collectors.toMap(Cohort.Builder::getId, Function.identity()));
    cohortRevisions.forEach(
        entry -> {
          String cohortId = entry.getKey();
          CohortRevision cohortRevision = entry.getValue().build();
          cohortsMap.get(cohortId).addRevision(cohortRevision);
        });

    // Preserve the order returned by the original query.
    return cohorts.stream()
        .map(c -> cohortsMap.get(c.getId()).build())
        .collect(Collectors.toList());
  }

  @WriteTransaction
  public void createRevision(String cohortId, CohortRevision cohortRevision) {
    // Create the review. The created and last_modified fields are set by the DB automatically on
    // insert.
    String sql =
        "INSERT INTO cohort_revision (cohort_id, id, version, is_most_recent, is_editable, created_by, last_modified_by, records_count) "
            + "VALUES (:cohort_id, :id, :version, :is_most_recent, :is_editable, :created_by, :last_modified_by, :records_count)";
    MapSqlParameterSource revisionParamSets =
        buildRevisionParam(
            cohortId,
            cohortRevision.getId(),
            cohortRevision.getVersion(),
            cohortRevision.isMostRecent(),
            cohortRevision.isEditable(),
            cohortRevision.getCreatedBy(),
            cohortRevision.getRecordsCount(),
            /* reviewId= */ null);
    JdbcUtils.insertRows(jdbcTemplate, "cohort_revision", sql, List.of(revisionParamSets));

    // Write the criteria group sections.
    updateCriteriaHelper(cohortRevision.getId(), cohortRevision.getSections());
  }

  @ReadTransaction
  public List<Pair<String, CohortRevision.Builder>> getRevisions(
      Set<String> cohortIds, boolean mostRecentOnly) {
    // Fetch the most recent cohort revisions. (cohort id -> cohort revision)
    String sql = COHORT_REVISION_SELECT_SQL + " WHERE cohort_id IN (:cohort_ids)";
    if (mostRecentOnly) {
      sql += " AND is_most_recent";
    }
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("cohort_ids", cohortIds);
    return jdbcTemplate.query(sql, params, COHORT_REVISION_ROW_MAPPER);
  }

  @ReadTransaction
  public void getCriteriaHelper(List<Pair<String, CohortRevision.Builder>> cohortRevisions) {
    // Fetch criteria group sections. (cohort revision id -> criteria group section)
    String sql =
        CRITERIA_GROUP_SECTION_SELECT_SQL
            + " WHERE cohort_revision_id IN (:cohort_revision_ids) "
            + "ORDER BY list_index ASC";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(
                "cohort_revision_ids",
                cohortRevisions.stream()
                    .map(cr -> cr.getValue().getId())
                    .collect(Collectors.toSet()));
    List<Pair<String, CohortRevision.CriteriaGroupSection.Builder>> criteriaGroupSections =
        jdbcTemplate.query(sql, params, CRITERIA_GROUP_SECTION_ROW_MAPPER);

    // Fetch criteria groups. (criteria group section -> criteria group)
    List<Pair<Pair<List<String>, Integer>, CohortRevision.CriteriaGroup.Builder>> criteriaGroups;
    if (criteriaGroupSections.isEmpty()) {
      criteriaGroups = Collections.emptyList();
    } else {
      sql =
          CRITERIA_GROUP_SELECT_SQL
              + " WHERE criteria_group_section_id IN (:criteria_group_section_ids) AND cohort_revision_id IN (:cohort_revision_ids) "
              + "ORDER BY list_index ASC";
      params =
          new MapSqlParameterSource()
              .addValue(
                  "criteria_group_section_ids",
                  criteriaGroupSections.stream()
                      .map(cgs -> cgs.getValue().getId())
                      .collect(Collectors.toSet()))
              .addValue(
                  "cohort_revision_ids",
                  cohortRevisions.stream()
                      .map(cr -> cr.getValue().getId())
                      .collect(Collectors.toSet()));
      criteriaGroups = jdbcTemplate.query(sql, params, CRITERIA_GROUP_ROW_MAPPER);
    }

    // Fetch criteria. (criteria group id -> criteria)
    List<Pair<List<String>, Criteria.Builder>> criterias;
    if (criteriaGroups.isEmpty()) {
      criterias = Collections.emptyList();
    } else {
      sql =
          CRITERIA_SELECT_SQL
              + " WHERE criteria_group_id IN (:criteria_group_ids) AND criteria_group_section_id IN (:criteria_group_section_ids) AND cohort_revision_id IN (:cohort_revision_ids) "
              + "ORDER BY list_index ASC";
      params =
          new MapSqlParameterSource()
              .addValue(
                  "criteria_group_ids",
                  criteriaGroups.stream()
                      .map(cg -> cg.getValue().getId())
                      .collect(Collectors.toSet()))
              .addValue(
                  "criteria_group_section_ids",
                  criteriaGroupSections.stream()
                      .map(cgs -> cgs.getValue().getId())
                      .collect(Collectors.toSet()))
              .addValue(
                  "cohort_revision_ids",
                  cohortRevisions.stream()
                      .map(cr -> cr.getValue().getId())
                      .collect(Collectors.toSet()));
      criterias = jdbcTemplate.query(sql, params, CRITERIA_ROW_MAPPER);
    }

    // Fetch criteria tags. (criteria -> tag)
    List<Pair<List<String>, Pair<String, String>>> tags;
    if (criterias.isEmpty()) {
      tags = Collections.emptyList();
    } else {
      sql =
          CRITERIA_TAG_SELECT_SQL
              + " WHERE criteria_id IN (:criteria_ids) AND criteria_group_id IN (:criteria_group_ids) AND criteria_group_section_id IN (:criteria_group_section_ids) AND cohort_revision_id IN (:cohort_revision_ids)";
      params =
          new MapSqlParameterSource()
              .addValue(
                  "criteria_ids",
                  criterias.stream().map(c -> c.getValue().getId()).collect(Collectors.toSet()))
              .addValue(
                  "criteria_group_ids",
                  criteriaGroups.stream()
                      .map(cg -> cg.getValue().getId())
                      .collect(Collectors.toSet()))
              .addValue(
                  "criteria_group_section_ids",
                  criteriaGroupSections.stream()
                      .map(cgs -> cgs.getValue().getId())
                      .collect(Collectors.toSet()))
              .addValue(
                  "cohort_revision_ids",
                  cohortRevisions.stream()
                      .map(cr -> cr.getValue().getId())
                      .collect(Collectors.toSet()));
      tags = jdbcTemplate.query(sql, params, CRITERIA_TAG_ROW_MAPPER);
    }

    // Put criteria tags in their respective criterias.
    Map<List<String>, Criteria.Builder> criteriasMap =
        criterias.stream()
            .collect(
                Collectors.toMap(
                    c -> {
                      List<String> uniqueId = new ArrayList<>();
                      uniqueId.add(c.getValue().getId());
                      uniqueId.addAll(c.getKey());
                      return uniqueId;
                    },
                    Pair::getValue));
    for (Pair<List<String>, Pair<String, String>> pair : tags) {
      List<String> criteriaId = pair.getKey();
      Pair<String, String> tag = pair.getValue();
      criteriasMap.get(criteriaId).addTag(tag.getKey(), tag.getValue());
    }

    // Put criteria into their respective criteria groups.
    Map<List<String>, CohortRevision.CriteriaGroup.Builder> criteriaGroupsMap =
        criteriaGroups.stream()
            .collect(
                Collectors.toMap(
                    cg -> {
                      List<String> uniqueId = new ArrayList<>();
                      uniqueId.add(cg.getValue().getId());
                      uniqueId.addAll(cg.getKey().getLeft());
                      return uniqueId;
                    },
                    Pair::getValue));
    for (Pair<List<String>, Criteria.Builder> pair : criterias) {
      List<String> criteriaGroupId = pair.getKey();
      Criteria criteria = pair.getValue().build();
      criteriaGroupsMap.get(criteriaGroupId).addCriteria(criteria);
    }

    // Put criteria groups into their respective criteria group sections.
    Map<List<String>, CohortRevision.CriteriaGroupSection.Builder> criteriaGroupSectionsMap =
        criteriaGroupSections.stream()
            .collect(
                Collectors.toMap(
                    cgs -> {
                      List<String> uniqueId = new ArrayList<>();
                      uniqueId.add(cgs.getValue().getId());
                      uniqueId.add(cgs.getKey());
                      return uniqueId;
                    },
                    Pair::getValue));
    for (Pair<Pair<List<String>, Integer>, CohortRevision.CriteriaGroup.Builder> pair :
        criteriaGroups) {
      List<String> criteriaGroupSectionId = pair.getKey().getLeft();
      Integer criteriaGroupSectionConditionIndex = pair.getKey().getRight();
      CohortRevision.CriteriaGroup criteriaGroup = pair.getValue().build();

      if (criteriaGroupSectionConditionIndex == null || criteriaGroupSectionConditionIndex == 0) {
        criteriaGroupSectionsMap.get(criteriaGroupSectionId).addCriteriaGroup(criteriaGroup);
      } else {
        criteriaGroupSectionsMap
            .get(criteriaGroupSectionId)
            .addSecondConditionCriteriaGroup(criteriaGroup);
      }
    }

    // Put criteria group sections into their respective cohort revisions.
    Map<String, CohortRevision.Builder> cohortRevisionsMap =
        cohortRevisions.stream()
            .collect(Collectors.toMap(cr -> cr.getValue().getId(), Pair::getValue));
    for (Pair<String, CohortRevision.CriteriaGroupSection.Builder> pair : criteriaGroupSections) {
      String cohortRevisionId = pair.getKey();
      CohortRevision.CriteriaGroupSection criteriaGroupSection = pair.getValue().build();
      cohortRevisionsMap.get(cohortRevisionId).addCriteriaGroupSection(criteriaGroupSection);
    }
  }

  @SuppressWarnings("checkstyle:NestedForDepth")
  private void updateCriteriaHelper(
      String cohortRevisionId, List<CohortRevision.CriteriaGroupSection> criteriaGroupSections) {
    // Delete any existing criteria group sections, criteria groups, criteria, and tags.
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("cohort_revision_id", cohortRevisionId);
    String sql =
        "DELETE FROM criteria_group_section WHERE cohort_revision_id = :cohort_revision_id";
    LOGGER.debug("DELETE criteria_group_section: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE criteria_group_section rowsAffected = {}", rowsAffected);

    sql = "DELETE FROM criteria_group WHERE cohort_revision_id = :cohort_revision_id";
    LOGGER.debug("DELETE criteria_group: {}", sql);
    rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE criteria_group rowsAffected = {}", rowsAffected);

    sql = "DELETE FROM criteria WHERE cohort_revision_id = :cohort_revision_id";
    LOGGER.debug("DELETE criteria: {}", sql);
    rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE criteria rowsAffected = {}", rowsAffected);

    sql = "DELETE FROM criteria_tag WHERE cohort_revision_id = :cohort_revision_id";
    LOGGER.debug("DELETE criteria_tag: {}", sql);
    rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE criteria_tag rowsAffected = {}", rowsAffected);

    // Write the criteria group sections, criteria groups, criteria, and tags.
    List<MapSqlParameterSource> sectionParamSets = new ArrayList<>();
    List<MapSqlParameterSource> groupParamSets = new ArrayList<>();
    List<MapSqlParameterSource> criteriaParamSets = new ArrayList<>();
    List<MapSqlParameterSource> tagParamSets = new ArrayList<>();
    buildParamsForUpdateCriteriaHelper(
        cohortRevisionId,
        criteriaGroupSections,
        sectionParamSets,
        groupParamSets,
        criteriaParamSets,
        tagParamSets);
    insertCriteriaRows(sectionParamSets, groupParamSets, criteriaParamSets, tagParamSets);
  }

  private void buildParamsForUpdateCriteriaHelper(
      String cohortRevisionId,
      List<CohortRevision.CriteriaGroupSection> criteriaGroupSections,
      List<MapSqlParameterSource> sectionParamSets,
      List<MapSqlParameterSource> groupParamSets,
      List<MapSqlParameterSource> criteriaParamSets,
      List<MapSqlParameterSource> tagParamSets) {
    for (int cgsListIndex = 0; cgsListIndex < criteriaGroupSections.size(); cgsListIndex++) {
      CohortRevision.CriteriaGroupSection cgs = criteriaGroupSections.get(cgsListIndex);
      sectionParamSets.add(
          new MapSqlParameterSource()
              .addValue("cohort_revision_id", cohortRevisionId)
              .addValue("id", cgs.getId())
              .addValue("display_name", cgs.getDisplayName())
              .addValue("operator", cgs.getOperator().name())
              .addValue(
                  "first_condition_reducing_operator",
                  cgs.getFirstConditionReducingOperator() == null
                      ? null
                      : cgs.getFirstConditionReducingOperator().name())
              .addValue(
                  "second_condition_reducing_operator",
                  cgs.getSecondConditionRedcuingOperator() == null
                      ? null
                      : cgs.getSecondConditionRedcuingOperator().name())
              .addValue(
                  "join_operator",
                  cgs.getJoinOperator() == null ? null : cgs.getJoinOperator().name())
              .addValue("join_operator_value", cgs.getJoinOperatorValue())
              .addValue("is_excluded", cgs.isExcluded())
              .addValue("is_disabled", cgs.isDisabled())
              .addValue("list_index", cgsListIndex));

      buildParamsForGroupCriteriaTag(
          cohortRevisionId,
          cgs.getId(),
          cgs.getCriteriaGroups(),
          0,
          groupParamSets,
          criteriaParamSets,
          tagParamSets);
      buildParamsForGroupCriteriaTag(
          cohortRevisionId,
          cgs.getId(),
          cgs.getSecondConditionCriteriaGroups(),
          1,
          groupParamSets,
          criteriaParamSets,
          tagParamSets);
    }
  }

  private void buildParamsForGroupCriteriaTag(
      String cohortRevisionId,
      String criteriaGroupSectionId,
      List<CohortRevision.CriteriaGroup> criteriaGroups,
      Integer criteriaGroupConditionIndex,
      List<MapSqlParameterSource> groupParamSets,
      List<MapSqlParameterSource> criteriaParamSets,
      List<MapSqlParameterSource> tagParamSets) {
    for (int cgListIndex = 0; cgListIndex < criteriaGroups.size(); cgListIndex++) {
      CohortRevision.CriteriaGroup cg = criteriaGroups.get(cgListIndex);
      groupParamSets.add(
          new MapSqlParameterSource()
              .addValue("cohort_revision_id", cohortRevisionId)
              .addValue("criteria_group_section_id", criteriaGroupSectionId)
              .addValue("id", cg.getId())
              .addValue("display_name", cg.getDisplayName())
              .addValue("list_index", cgListIndex)
              .addValue("condition_index", criteriaGroupConditionIndex)
              .addValue("is_disabled", cg.isDisabled()));

      for (int cListIndex = 0; cListIndex < cg.getCriteria().size(); cListIndex++) {
        Criteria c = cg.getCriteria().get(cListIndex);
        criteriaParamSets.add(
            new MapSqlParameterSource()
                .addValue("cohort_revision_id", cohortRevisionId)
                .addValue("criteria_group_section_id", criteriaGroupSectionId)
                .addValue("criteria_group_id", cg.getId())
                .addValue("id", c.getId())
                .addValue("display_name", c.getDisplayName())
                .addValue("plugin_name", c.getPluginName())
                .addValue("plugin_version", c.getPluginVersion())
                .addValue("selector_or_modifier_name", c.getSelectorOrModifierName())
                .addValue("selection_data", c.getSelectionData())
                .addValue("ui_config", c.getUiConfig())
                .addValue("list_index", cListIndex));

        for (Map.Entry<String, String> t : c.getTags().entrySet()) {
          tagParamSets.add(
              new MapSqlParameterSource()
                  .addValue("cohort_revision_id", cohortRevisionId)
                  .addValue("criteria_group_section_id", criteriaGroupSectionId)
                  .addValue("criteria_group_id", cg.getId())
                  .addValue("criteria_id", c.getId())
                  .addValue("key", t.getKey())
                  .addValue("value", t.getValue()));
        }
      }
    }
  }

  private void insertCohortRows(List<MapSqlParameterSource> cohortParamSets) {
    // Write the cohort. The created and last_modified fields are set by the DB automatically on
    // insert.
    String sql =
        "INSERT INTO cohort (study_id, id, underlay, created_by, last_modified_by, display_name, description, is_deleted) "
            + "VALUES (:study_id, :id, :underlay, :created_by, :last_modified_by, :display_name, :description, false)";
    JdbcUtils.insertRows(jdbcTemplate, "cohort", sql, cohortParamSets);
  }

  private void insertCriteriaRows(
      List<MapSqlParameterSource> sectionParamSets,
      List<MapSqlParameterSource> groupParamSets,
      List<MapSqlParameterSource> criteriaParamSets,
      List<MapSqlParameterSource> tagParamSets) {
    String sql =
        "INSERT INTO criteria_group_section (cohort_revision_id, id, display_name, operator, is_excluded, is_disabled, first_condition_reducing_operator, second_condition_reducing_operator, join_operator, join_operator_value, list_index) "
            + "VALUES (:cohort_revision_id, :id, :display_name, :operator, :is_excluded, :is_disabled, :first_condition_reducing_operator, :second_condition_reducing_operator, :join_operator, :join_operator_value, :list_index)";
    JdbcUtils.insertRows(jdbcTemplate, "criteria_group_section", sql, sectionParamSets);

    sql =
        "INSERT INTO criteria_group (cohort_revision_id, criteria_group_section_id, id, display_name, condition_index, list_index, is_disabled) "
            + "VALUES (:cohort_revision_id, :criteria_group_section_id, :id, :display_name, :condition_index, :list_index, :is_disabled)";
    JdbcUtils.insertRows(jdbcTemplate, "criteria_group", sql, groupParamSets);

    sql =
        "INSERT INTO criteria (cohort_revision_id, criteria_group_section_id, criteria_group_id, id, display_name, plugin_name, plugin_version, selector_or_modifier_name, selection_data, ui_config, list_index) "
            + "VALUES (:cohort_revision_id, :criteria_group_section_id, :criteria_group_id, :id, :display_name, :plugin_name, :plugin_version, :selector_or_modifier_name, :selection_data, :ui_config, :list_index)";
    JdbcUtils.insertRows(jdbcTemplate, "criteria", sql, criteriaParamSets);

    sql =
        "INSERT INTO criteria_tag (cohort_revision_id, criteria_group_section_id, criteria_group_id, criteria_id, criteria_key, criteria_value) "
            + "VALUES (:cohort_revision_id, :criteria_group_section_id, :criteria_group_id, :criteria_id, :key, :value)";
    JdbcUtils.insertRows(jdbcTemplate, "criteria_tag", sql, tagParamSets);
  }

  static MapSqlParameterSource buildCohortParam(
      String studyId,
      String id,
      String underlay,
      String userEmail,
      String displayName,
      String description) {
    return new MapSqlParameterSource()
        .addValue("study_id", studyId)
        .addValue("id", id)
        .addValue("underlay", underlay)
        .addValue("created_by", userEmail)
        .addValue("last_modified_by", userEmail)
        .addValue("display_name", displayName)
        .addValue("description", description);
  }

  static MapSqlParameterSource buildRevisionParam(
      String cohortId,
      String id,
      int version,
      boolean isMostRecent,
      boolean isEditable,
      String userEmail,
      Long recordsCount,
      String reviewId) {
    return new MapSqlParameterSource()
        .addValue("cohort_id", cohortId)
        .addValue("id", id)
        .addValue("version", version)
        .addValue("is_most_recent", isMostRecent)
        .addValue("is_editable", isEditable)
        .addValue("created_by", userEmail)
        .addValue("last_modified_by", userEmail)
        .addValue("records_count", recordsCount)
        .addValue("review_id", reviewId);
  }
}
