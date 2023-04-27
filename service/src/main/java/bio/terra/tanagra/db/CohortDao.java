package bio.terra.tanagra.db;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.filtervariable.BooleanAndOrFilterVariable;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.CohortRevision;
import bio.terra.tanagra.service.model.Criteria;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
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

@Component
public class CohortDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(CohortDao.class);

  // SQL query and row mapper for reading a cohort.
  private static final String COHORT_SELECT_SQL =
      "SELECT id, underlay_name, created, created_by, last_modified, last_modified_by, display_name, description FROM cohort";
  private static final RowMapper<Cohort.Builder> COHORT_ROW_MAPPER =
      (rs, rowNum) ->
          Cohort.builder()
              .id(rs.getString("id"))
              .underlayName(rs.getString("underlay_name"))
              .created(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
              .createdBy(rs.getString("created_by"))
              .lastModified(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
              .lastModifiedBy(rs.getString("last_modified_by"))
              .displayName(rs.getString("display_name"))
              .description(rs.getString("description"));

  // SQL query and row mapper for reading a cohort revision.
  private static final String COHORT_REVISION_SELECT_SQL =
      "SELECT cohort_id, id, version, is_most_recent, is_editable, created, created_by, last_modified, last_modified_by FROM cohort_revision";
  private static final RowMapper<Pair<String, CohortRevision.Builder>> COHORT_REVISION_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              rs.getString("cohort_id"),
              CohortRevision.builder()
                  .id(rs.getString("id"))
                  .version(rs.getInt("version"))
                  .setIsMostRecent(rs.getBoolean("is_most_recent"))
                  .setIsEditable(rs.getBoolean("is_editable"))
                  .created(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("created")))
                  .createdBy(rs.getString("created_by"))
                  .lastModified(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("last_modified")))
                  .lastModifiedBy(rs.getString("last_modified_by")));

  // SQL query and row mapper for reading a criteria group section.
  private static final String CRITERIA_GROUP_SECTION_SELECT_SQL =
      "SELECT cohort_revision_id, id, display_name, operator, is_excluded FROM criteria_group_section";
  private static final RowMapper<Pair<String, CohortRevision.CriteriaGroupSection.Builder>>
      CRITERIA_GROUP_SECTION_ROW_MAPPER =
          (rs, rowNum) ->
              Pair.of(
                  rs.getString("cohort_revision_id"),
                  CohortRevision.CriteriaGroupSection.builder()
                      .id(rs.getString("id"))
                      .displayName(rs.getString("display_name"))
                      .operator(
                          BooleanAndOrFilterVariable.LogicalOperator.valueOf(
                              rs.getString("operator")))
                      .setIsExcluded(rs.getBoolean("is_excluded")));

  // SQL query and row mapper for reading a criteria group.
  private static final String CRITERIA_GROUP_SELECT_SQL =
      "SELECT criteria_group_section_id, id, display_name, entity, group_by_count_operator, group_by_count_value FROM criteria_group";
  private static final RowMapper<Pair<String, CohortRevision.CriteriaGroup.Builder>>
      CRITERIA_GROUP_ROW_MAPPER =
          (rs, rowNum) ->
              Pair.of(
                  rs.getString("criteria_group_section_id"),
                  CohortRevision.CriteriaGroup.builder()
                      .id(rs.getString("id"))
                      .displayName(rs.getString("display_name"))
                      .entity(rs.getString("entity"))
                      .groupByCountOperator(
                          BinaryFilterVariable.BinaryOperator.valueOf(
                              rs.getString("group_by_count_operator")))
                      .groupByCountValue(rs.getInt("group_by_count_value")));

  // SQL query and row mapper for reading a criteria.
  private static final String CRITERIA_SELECT_SQL =
      "SELECT criteria_group_id, id, display_name, plugin_name, selection_data, ui_config FROM criteria";
  private static final RowMapper<Pair<String, Criteria.Builder>> CRITERIA_ROW_MAPPER =
      (rs, rowNum) ->
          Pair.of(
              rs.getString("criteria_group_id"),
              Criteria.builder()
                  .id(rs.getString("criteria_group_id"))
                  .displayName(rs.getString("display_name"))
                  .pluginName(rs.getString("plugin_name"))
                  .selectionData(rs.getString("selection_data"))
                  .uiConfig(rs.getString("ui_config")));

  // SQL query and row mapper for reading a criteria tag.
  private static final String CRITERIA_TAG_SELECT_SQL = "SELECT criteria_id, tag FROM criteria_tag";
  private static final RowMapper<Pair<String, String>> CRITERIA_TAG_ROW_MAPPER =
      (rs, rowNum) -> Pair.of(rs.getString("criteria_id"), rs.getString("tag"));

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public CohortDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @ReadTransaction
  public List<Cohort> getAllCohorts(String studyId, int offset, int limit) {
    String sql =
        COHORT_SELECT_SQL
            + " WHERE study_id = :study_id ORDER BY display_name OFFSET :offset LIMIT :limit";
    LOGGER.debug("GET ALL cohorts: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("offset", offset)
            .addValue("limit", limit);
    List<Cohort> cohorts = getCohortsHelper(sql, params);
    LOGGER.debug("GET ALL cohorts numFound = {}", cohorts.size());
    return cohorts;
  }

  @ReadTransaction
  public List<Cohort> getCohortsMatchingList(Set<String> ids, int offset, int limit) {
    String sql =
        COHORT_SELECT_SQL + " WHERE id IN (:ids) ORDER BY display_name OFFSET :offset LIMIT :limit";
    LOGGER.debug("GET MATCHING cohorts: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("ids", ids.stream().reduce(",", String::concat))
            .addValue("offset", offset)
            .addValue("limit", limit);
    List<Cohort> cohorts = getCohortsHelper(sql, params);
    LOGGER.debug("GET MATCHING cohorts numFound = {}", cohorts.size());
    return cohorts;
  }

  @ReadTransaction
  public Cohort getCohort(String id) {
    // Fetch cohort.
    String sql = COHORT_SELECT_SQL + " WHERE id = :id";
    LOGGER.debug("GET cohort: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    List<Cohort> cohorts = getCohortsHelper(sql, params);
    LOGGER.debug("GET cohort numFound = {}", cohorts.size());

    // Make sure there's only one cohort returned for this id.
    if (cohorts.size() == 0) {
      throw new NotFoundException("Cohort not found " + id);
    } else if (cohorts.size() > 1) {
      throw new SystemException("Multiple cohorts found " + id);
    }
    return cohorts.get(0);
  }

  @WriteTransaction
  public void deleteCohort(String id) {
    String sql = "DELETE FROM cohort WHERE id = :id";
    LOGGER.debug("DELETE cohort: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE cohort rowsAffected = {}", rowsAffected);
  }

  @WriteTransaction
  public void createCohort(String studyId, Cohort cohort) {
    // Write the cohort. The created and last_modified fields are set by the DB automatically on
    // insert.
    String sql =
        "INSERT INTO cohort (study_id, id, underlay_name, created_by, display_name, description) "
            + "VALUES (:study_id, :id, :underlay_name, :created_by, :display_name, :description)";
    LOGGER.debug("CREATE cohort: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("study_id", studyId)
            .addValue("id", cohort.getId())
            .addValue("underlay_name", cohort.getUnderlayName())
            .addValue("created_by", cohort.getCreatedBy())
            .addValue("display_name", cohort.getDisplayName())
            .addValue("description", cohort.getDescription());
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("CREATE cohort rowsAffected = {}", rowsAffected);

    // Write the first cohort revision. The created and last_modified fields are set by the DB
    // automatically on insert.
    sql =
        "INSERT INTO cohort_revision (cohort_id, id, version, is_most_recent, is_editable, created_by, last_modified_by) "
            + "VALUES (:cohort_id, :id, :version, :is_most_recent, :is_editable, :created_by, :last_modified_by)";
    LOGGER.debug("CREATE cohort_revision: {}", sql);
    CohortRevision cohortRevision = cohort.getMostRecentRevision();
    params =
        new MapSqlParameterSource()
            .addValue("cohort_id", cohort.getId())
            .addValue("id", cohortRevision.getId())
            .addValue("version", cohortRevision.getVersion())
            .addValue("is_most_recent", cohortRevision.isMostRecent())
            .addValue("is_editable", cohortRevision.isEditable())
            .addValue("created_by", cohortRevision.getCreatedBy())
            .addValue("last_modified_by", cohortRevision.getLastModifiedBy());
    rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("CREATE cohort_revision rowsAffected = {}", rowsAffected);

    // Write the criteria group sections.
    updateCriteriaHelper(cohortRevision.getId(), cohortRevision.getSections());
  }

  @WriteTransaction
  public void updateCohort(
      String id,
      String lastModifiedBy,
      String displayName,
      String description,
      List<CohortRevision.CriteriaGroupSection> criteriaGroupSections) {
    // Update the cohort: display name, description, last modified, last modified by.
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("last_modified", Timestamp.from(Instant.now()))
            .addValue("last_modified_by", lastModifiedBy);
    if (displayName != null) {
      params.addValue("display_name", displayName);
    }
    if (description != null) {
      params.addValue("description", description);
    }
    String sql =
        String.format("UPDATE cohort SET %s WHERE id = :id", DbUtils.setColumnsClause(params));
    LOGGER.debug("UPDATE cohort: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("UPDATE cohort rowsAffected = {}", rowsAffected);

    // Update the most recent cohort revision.
    if (criteriaGroupSections != null && !criteriaGroupSections.isEmpty()) {
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
              .addValue("last_modified", Timestamp.from(Instant.now()))
              .addValue("last_modified_by", lastModifiedBy)
              .addValue("id", cohortRevisionId);
      rowsAffected = jdbcTemplate.update(sql, params);
      LOGGER.debug("UPDATE cohort_revision rowsAffected = {}", rowsAffected);

      // Write the criteria group sections.
      updateCriteriaHelper(cohortRevisionId, criteriaGroupSections);
    }
  }

  private List<Cohort> getCohortsHelper(String cohortsSql, MapSqlParameterSource cohortsParams) {
    // Fetch cohorts.
    Map<String, Cohort.Builder> cohorts =
        jdbcTemplate.query(cohortsSql, cohortsParams, COHORT_ROW_MAPPER).stream()
            .collect(Collectors.toMap(Cohort.Builder::getId, Function.identity()));

    // Fetch the most recent cohort revisions. (cohort id -> cohort revision)
    String sql =
        COHORT_REVISION_SELECT_SQL + " WHERE cohort_id IN (:cohort_ids) AND is_most_recent";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(
                "cohort_ids",
                cohorts.values().stream().map(c -> c.getId()).reduce(",", String::concat));
    Map<String, CohortRevision.Builder> cohortRevisions =
        jdbcTemplate.query(sql, params, COHORT_REVISION_ROW_MAPPER).stream()
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    // Fetch criteria group sections. (cohort revision id -> criteria group section)
    sql = CRITERIA_GROUP_SECTION_SELECT_SQL + " WHERE cohort_revision_id IN (:cohort_revision_ids)";
    params =
        new MapSqlParameterSource()
            .addValue(
                "cohort_revision_ids",
                cohortRevisions.values().stream()
                    .map(cgs -> cgs.getId())
                    .reduce(",", String::concat));
    Map<String, CohortRevision.CriteriaGroupSection.Builder> criteriaGroupSections =
        jdbcTemplate.query(sql, params, CRITERIA_GROUP_SECTION_ROW_MAPPER).stream()
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    // Fetch criteria groups. (criteria group section -> criteria group)
    sql =
        CRITERIA_GROUP_SELECT_SQL
            + " WHERE criteria_group_section_id IN (:criteria_group_section_ids)";
    params =
        new MapSqlParameterSource()
            .addValue(
                "criteria_group_section_ids",
                criteriaGroupSections.values().stream()
                    .map(cgs -> cgs.getId())
                    .reduce(",", String::concat));
    Map<String, CohortRevision.CriteriaGroup.Builder> criteriaGroups =
        jdbcTemplate.query(sql, params, CRITERIA_GROUP_ROW_MAPPER).stream()
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    // Fetch criteria. (criteria group id -> criteria)
    sql = CRITERIA_SELECT_SQL + " WHERE criteria_group_id IN (:criteria_group_ids)";
    params =
        new MapSqlParameterSource()
            .addValue(
                "criteria_group_ids",
                criteriaGroups.values().stream().map(cg -> cg.getId()).reduce(",", String::concat));
    Map<String, Criteria.Builder> criterias =
        jdbcTemplate.query(sql, params, CRITERIA_ROW_MAPPER).stream()
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    // Fetch criteria tags. (criteria id -> criteria tag)
    sql = CRITERIA_TAG_SELECT_SQL + "WHERE criteria_id IN (:criteria_ids)";
    params =
        new MapSqlParameterSource()
            .addValue(
                "criteria_ids",
                criterias.values().stream().map(c -> c.getId()).reduce(",", String::concat));
    Map<String, String> criteriaTags =
        jdbcTemplate.query(sql, params, CRITERIA_TAG_ROW_MAPPER).stream()
            .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    // Assemble criteria tags into criteria.
    criteriaTags.entrySet().stream()
        .forEach(
            entry -> {
              String criteriaId = entry.getKey();
              String criteriaTag = entry.getValue();
              criterias.get(criteriaId).addTag(criteriaTag);
            });

    // Assemble criteria into criteria groups.
    criterias.entrySet().stream()
        .forEach(
            entry -> {
              String criteriaGroupId = entry.getKey();
              Criteria criteria = entry.getValue().build();
              criteriaGroups.get(criteriaGroupId).addCriteria(criteria);
            });

    // Assemble criteria groups into criteria group sections.
    criteriaGroups.entrySet().stream()
        .forEach(
            entry -> {
              String criteriaGroupSectionId = entry.getKey();
              CohortRevision.CriteriaGroup criteriaGroup = entry.getValue().build();
              criteriaGroupSections.get(criteriaGroupSectionId).addCriteriaGroup(criteriaGroup);
            });

    // Assemble criteria group sections into cohort revisions.
    criteriaGroupSections.entrySet().stream()
        .forEach(
            entry -> {
              String cohortRevisionId = entry.getKey();
              CohortRevision.CriteriaGroupSection criteriaGroupSection = entry.getValue().build();
              cohortRevisions.get(cohortRevisionId).addCriteriaGroupSection(criteriaGroupSection);
            });

    // Assemble cohort revisions into cohorts.
    cohortRevisions.entrySet().stream()
        .forEach(
            entry -> {
              String cohortId = entry.getKey();
              CohortRevision cohortRevision = entry.getValue().build();
              cohorts.get(cohortId).addRevision(cohortRevision);
            });

    return cohorts.values().stream().map(Cohort.Builder::build).collect(Collectors.toList());
  }

  private void updateCriteriaHelper(
      String cohortRevisionId, List<CohortRevision.CriteriaGroupSection> criteriaGroupSections) {
    // Delete any existing criteria group sections, criteria groups, criteria, and criteria tags.
    String sql =
        "DELETE FROM criteria_group_section WHERE cohort_revision_id = :cohort_revision_id";
    LOGGER.debug("DELETE criteria_group_section: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("cohort_revision_id", cohortRevisionId);
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("DELETE criteria_group_section rowsAffected = {}", rowsAffected);

    // Write the criteria group sections, criteria groups, criteria, and criteria tags.
    List<MapSqlParameterSource> sectionParamSets = new ArrayList<>();
    List<MapSqlParameterSource> groupParamSets = new ArrayList<>();
    List<MapSqlParameterSource> criteriaParamSets = new ArrayList<>();
    List<MapSqlParameterSource> tagParamSets = new ArrayList<>();

    criteriaGroupSections.stream()
        .forEach(
            cgs -> {
              sectionParamSets.add(
                  new MapSqlParameterSource()
                      .addValue("cohort_revision_id", cohortRevisionId)
                      .addValue("id", cgs.getId())
                      .addValue("display_name", cgs.getDisplayName())
                      .addValue("operator", cgs.getOperator().name())
                      .addValue("is_excluded", cgs.isExcluded()));

              cgs.getCriteriaGroups().stream()
                  .forEach(
                      cg -> {
                        groupParamSets.add(
                            new MapSqlParameterSource()
                                .addValue("criteria_group_section_id", cgs.getId())
                                .addValue("id", cg.getId())
                                .addValue("display_name", cg.getDisplayName())
                                .addValue("entity", cg.getEntity())
                                .addValue("group_by_count_operator", cg.getGroupByCountOperator())
                                .addValue("group_by_count_value", cg.getGroupByCountValue()));

                        cg.getCriteria().stream()
                            .forEach(
                                c -> {
                                  criteriaParamSets.add(
                                      new MapSqlParameterSource()
                                          .addValue("criteria_group_id", cg.getId())
                                          .addValue("id", c.getId())
                                          .addValue("display_name", c.getDisplayName())
                                          .addValue("plugin_name", c.getPluginName())
                                          .addValue("selection_data", c.getSelectionData())
                                          .addValue("ui_config", c.getUiConfig()));

                                  c.getTags().stream()
                                      .forEach(
                                          ct ->
                                              tagParamSets.add(
                                                  new MapSqlParameterSource()
                                                      .addValue("criteria_id", c.getId())
                                                      .addValue("tag", ct)));
                                });
                      });
            });

    sql =
        "INSERT INTO criteria_group_section (cohort_revision_id, id, display_name, operator, is_excluded) "
            + "VALUES (:cohort_revision_id, :id, :display_name, :operator, :is_excluded)";
    LOGGER.debug("CREATE criteria_group_section: {}", sql);
    rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(
                    sql, sectionParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE criteria_group_section rowsAffected = {}", rowsAffected);

    sql =
        "INSERT INTO criteria_group (criteria_group_section_id, id, display_name, entity, group_by_count_operator, group_by_count_value) "
            + "VALUES (:criteria_group_section_id, :id, :display_name, :entity, :group_by_count_operator, :group_by_count_value)";
    LOGGER.debug("CREATE criteria_group: {}", sql);
    rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(sql, groupParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE criteria_group rowsAffected = {}", rowsAffected);

    sql =
        "INSERT INTO criteria (criteria_group_id, id, display_name, plugin_name, selection_data, ui_config) "
            + "VALUES (:criteria_group_id, :id, :display_name, :plugin_name, :selection_data, :ui_config)";
    LOGGER.debug("CREATE criteria: {}", sql);
    rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(sql, groupParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE criteria rowsAffected = {}", rowsAffected);

    sql = "INSERT INTO criteria_tag (criteria_id, tag) " + "VALUES (:criteria_id, :tag)";
    LOGGER.debug("CREATE criteria_tag: {}", sql);
    rowsAffected =
        Arrays.stream(
                jdbcTemplate.batchUpdate(sql, groupParamSets.toArray(new MapSqlParameterSource[0])))
            .sum();
    LOGGER.debug("CREATE criteria_tag rowsAffected = {}", rowsAffected);
  }
}
