package bio.terra.tanagra.db;

import static bio.terra.tanagra.db.StudyDao.PROPERTY_ROW_MAPPER;
import static bio.terra.tanagra.db.StudyDao.PROPERTY_SELECT_SQL;

import bio.terra.common.db.ReadTransaction;
import bio.terra.common.db.WriteTransaction;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.service.artifact.*;
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
public class ActivityLogDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(ActivityLogDao.class);

  // SQL query and row mapper for reading an activity log entry.
  private static final String ACTIVITY_LOG_SELECT_SQL =
      "SELECT id, user_email, logged, version_git_tag, version_git_hash, version_build, activity_type, export_model FROM activity_log";
  private static final RowMapper<ActivityLog.Builder> ACTIVITY_LOG_ROW_MAPPER =
      (rs, rowNum) ->
          ActivityLog.builder()
              .id(rs.getString("id"))
              .userEmail(rs.getString("user_email"))
              .logged(DbUtils.timestampToOffsetDateTime(rs.getTimestamp("logged")))
              .versionGitTag(rs.getString("version_git_tag"))
              .versionGitHash(rs.getString("version_git_hash"))
              .versionBuild(rs.getString("version_build"))
              .type(ActivityLog.Type.valueOf(rs.getString("activity_type")))
              .exportModel(rs.getString("export_model"));

  // SQL query and row mapper for reading an activity log entry resource.
  private static final String ACTIVITY_LOG_RESOURCE_SELECT_SQL =
      "SELECT alr.activity_log_id, alr.stable_index, alr.resource_type, alr.study_id, alr.cohort_id, alr.cohort_revision_id, alr.review_id,"
          + "s.display_name AS study_display_name, c.display_name AS cohort_display_name, r.display_name AS review_display_name "
          + "FROM activity_log_resource AS alr "
          + "LEFT JOIN study AS s ON s.id = alr.study_id "
          + "LEFT JOIN cohort AS c ON c.id = alr.cohort_id "
          + "LEFT JOIN review AS r ON r.id = alr.review_id";
  private static final RowMapper<Pair<String, ActivityLogResource.Builder>>
      ACTIVITY_LOG_RESOURCE_ROW_MAPPER =
          (rs, rowNum) ->
              Pair.of(
                  rs.getString("activity_log_id"),
                  ActivityLogResource.builder()
                      .type(ActivityLogResource.Type.valueOf(rs.getString("resource_type")))
                      .studyId(rs.getString("study_id"))
                      .cohortId(rs.getString("cohort_id"))
                      .cohortRevisionId(rs.getString("cohort_revision_id"))
                      .reviewId(rs.getString("review_id"))
                      .studyDisplayName(rs.getString("study_display_name"))
                      .cohortDisplayName(rs.getString("cohort_display_name"))
                      .reviewDisplayName(rs.getString("review_display_name")));

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public ActivityLogDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @WriteTransaction
  public void createActivityLog(ActivityLog activityLog) {
    String sql =
        "INSERT INTO activity_log (id, user_email, version_git_tag, version_git_hash, version_build, activity_type, export_model) "
            + "VALUES (:id, :user_email, :version_git_tag, :version_git_hash, :version_build, :activity_type, :export_model)";
    LOGGER.debug("CREATE activity log: {}", sql);
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("id", activityLog.getId())
            .addValue("user_email", activityLog.getUserEmail())
            .addValue("version_git_tag", activityLog.getVersionGitTag())
            .addValue("version_git_hash", activityLog.getVersionGitHash())
            .addValue("version_build", activityLog.getVersionBuild())
            .addValue("activity_type", activityLog.getType().name())
            .addValue("export_model", activityLog.getExportModel());
    int rowsAffected = jdbcTemplate.update(sql, params);
    LOGGER.debug("CREATE activity log rowsAffected = {}", rowsAffected);

    if (!activityLog.getResources().isEmpty()) {
      sql =
          "INSERT INTO activity_log_resource (activity_log_id, stable_index, resource_type, study_id, cohort_id, cohort_revision_id, review_id) "
              + "VALUES (:activity_log_id, :stable_index, :resource_type, :study_id, :cohort_id, :cohort_revision_id, :review_id)";
      LOGGER.debug("CREATE activity log resource: {}", sql);
      List<MapSqlParameterSource> resourceParamSets = new ArrayList<>();
      for (int i = 0; i < activityLog.getResources().size(); i++) {
        ActivityLogResource resource = activityLog.getResources().get(i);
        resourceParamSets.add(
            new MapSqlParameterSource()
                .addValue("activity_log_id", activityLog.getId())
                .addValue("stable_index", i)
                .addValue("resource_type", resource.getType().name())
                .addValue("study_id", resource.getStudyId())
                .addValue("cohort_id", resource.getCohortId())
                .addValue("cohort_revision_id", resource.getCohortRevisionId())
                .addValue("review_id", resource.getReviewId()));
      }
      rowsAffected =
          Arrays.stream(
                  jdbcTemplate.batchUpdate(
                      sql, resourceParamSets.toArray(new MapSqlParameterSource[0])))
              .sum();
      LOGGER.debug("CREATE activity log resource rowsAffected = {}", rowsAffected);
    }
  }

  @ReadTransaction
  public List<ActivityLog> getAllActivityLogs(
      int offset,
      int limit,
      String userEmailFilter,
      boolean userEmailExactMatch,
      ActivityLog.Type activityTypeFilter,
      ActivityLogResource.Type resourceTypeFilter) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("offset", offset).addValue("limit", limit);
    String filterSql =
        renderSqlForActivityLogFilter(
            userEmailFilter, userEmailExactMatch, activityTypeFilter, resourceTypeFilter, params);
    String sql =
        ACTIVITY_LOG_SELECT_SQL
            + (filterSql.isEmpty() ? "" : " WHERE " + filterSql)
            + " ORDER BY logged DESC LIMIT :limit OFFSET :offset";
    LOGGER.debug("GET all activity logs: {}", sql);
    List<ActivityLog> activityLogs = getActivityLogsHelper(sql, params);
    LOGGER.debug("GET ALL activity logs numFound = {}", activityLogs.size());
    return activityLogs;
  }

  /** Convert an activity log filter object into a SQL WHERE clause. */
  private String renderSqlForActivityLogFilter(
      String userEmailFilter,
      boolean userEmailExactMatch,
      ActivityLog.Type activityTypeFilter,
      ActivityLogResource.Type resourceTypeFilter,
      MapSqlParameterSource params) {
    if ((userEmailFilter == null || userEmailFilter.isEmpty())
        && activityTypeFilter == null
        && resourceTypeFilter == null) {
      return "";
    }

    // Filter on the userEmail, activityType, and/or resourceType.
    List<String> whereConditions = new ArrayList<>();
    if (userEmailFilter != null && !userEmailFilter.isEmpty()) {
      if (userEmailExactMatch) {
        whereConditions.add("user_email = :user_email_filter");
        params.addValue("user_email_filter", userEmailFilter);
      } else {
        whereConditions.add("user_email LIKE :user_email_filter");
        params.addValue("user_email_filter", "%" + userEmailFilter + "%");
      }
    }
    if (activityTypeFilter != null) {
      whereConditions.add("activity_type = :activity_type_filter");
      params.addValue("activity_type_filter", activityTypeFilter.name());
    }
    if (resourceTypeFilter != null) {
      whereConditions.add(
          "id IN (SELECT activity_log_id FROM activity_log_resource WHERE activity_log_id = id AND resource_type = :resource_type_filter)");
      params.addValue("resource_type_filter", resourceTypeFilter.name());
    }

    // Build a WHERE clause ready string.
    return String.join(" AND ", whereConditions);
  }

  @ReadTransaction
  public ActivityLog getActivityLog(String activityLogId) {
    String sql = ACTIVITY_LOG_SELECT_SQL + " WHERE id = :id";
    LOGGER.debug("GET activity log: {}", sql);
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", activityLogId);
    List<ActivityLog> activityLogs = getActivityLogsHelper(sql, params);
    LOGGER.debug("GET activity log numFound = {}", activityLogs.size());

    // Make sure there's only one activity log returned for this id.
    if (activityLogs.isEmpty()) {
      throw new NotFoundException("Activity log not found " + activityLogId);
    } else if (activityLogs.size() > 1) {
      throw new SystemException("Multiple activity logs found " + activityLogId);
    }
    return activityLogs.get(0);
  }

  private List<ActivityLog> getActivityLogsHelper(
      String activityLogsSql, MapSqlParameterSource activityLogsParams) {
    // Fetch activity logs.
    List<ActivityLog.Builder> activityLogs =
        jdbcTemplate.query(activityLogsSql, activityLogsParams, ACTIVITY_LOG_ROW_MAPPER);
    if (activityLogs.isEmpty()) {
      return Collections.emptyList();
    }

    // Fetch the activity log resources.
    String sql =
        ACTIVITY_LOG_RESOURCE_SELECT_SQL
            + " WHERE activity_log_id IN (:activity_log_ids) ORDER BY stable_index ASC";
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue(
                "activity_log_ids",
                activityLogs.stream().map(al -> al.getId()).collect(Collectors.toSet()));
    List<Pair<String, ActivityLogResource.Builder>> resources =
        jdbcTemplate.query(sql, params, ACTIVITY_LOG_RESOURCE_ROW_MAPPER);

    // Fetch the study properties. (study id -> property)
    Set<String> studyIds = new HashSet<>();
    resources.stream()
        .forEach(
            pair -> {
              ActivityLogResource.Builder resource = pair.getValue();
              if (resource.getStudyId() != null) {
                studyIds.add(resource.getStudyId());
              }
            });
    sql = PROPERTY_SELECT_SQL + " WHERE study_id IN (:study_ids)";
    params = new MapSqlParameterSource().addValue("study_ids", studyIds);
    List<Pair<String, Pair<String, String>>> properties =
        jdbcTemplate.query(sql, params, PROPERTY_ROW_MAPPER);

    // Put study properties into their respective activity log resources.
    resources.stream()
        .forEach(
            resourcePair -> {
              ActivityLogResource.Builder resource = resourcePair.getValue();
              if (resource.getStudyId() != null) {
                properties.stream()
                    .forEach(
                        propertyPair -> {
                          String studyId = propertyPair.getKey();
                          if (studyId.equals(resource.getStudyId())) {
                            Pair<String, String> studyProperty = propertyPair.getValue();
                            resource.addStudyProperty(
                                studyProperty.getKey(), studyProperty.getValue());
                          }
                        });
              }
            });

    // Put the resources into their respective activity logs.
    Map<String, ActivityLog.Builder> activityLogsMap =
        activityLogs.stream()
            .collect(Collectors.toMap(ActivityLog.Builder::getId, Function.identity()));
    resources.stream()
        .forEach(
            entry -> {
              String activityLogId = entry.getKey();
              ActivityLogResource.Builder resource = entry.getValue();
              activityLogsMap.get(activityLogId).addResource(resource.build());
            });

    // Preserve the order returned by the original query.
    return activityLogs.stream()
        .map(al -> activityLogsMap.get(al.getId()).build())
        .collect(Collectors.toList());
  }

  @WriteTransaction
  public void deleteAllActivityLogs() {
    LOGGER.warn("Deleting all activity logs. This should only happen during testing.");
    String sql = "DELETE FROM activity_log";
    LOGGER.debug("DELETE activity log: {}", sql);
    int rowsAffected = jdbcTemplate.update(sql, new MapSqlParameterSource());
    LOGGER.debug("DELETE activity log rowsAffected = {}", rowsAffected);
  }
}
