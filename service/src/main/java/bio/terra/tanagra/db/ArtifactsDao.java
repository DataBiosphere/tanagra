package bio.terra.tanagra.db;

import bio.terra.common.db.WriteTransaction;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.Review;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

// A Dao for all artifacts: this is needed to avoid circular dependency errors
@Component
public class ArtifactsDao {
  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final CohortDao cohortDao;
  private final ReviewDao reviewDao;

  @Autowired
  public ArtifactsDao(
      NamedParameterJdbcTemplate jdbcTemplate, CohortDao cohortDao, ReviewDao reviewDao) {
    this.jdbcTemplate = jdbcTemplate;
    this.cohortDao = cohortDao;
    this.reviewDao = reviewDao;
  }

  /**
   * Clones a cohort and all related artifacts by copying DB rows. Does not include validations.
   * Returns the id of the new cohort
   */
  @WriteTransaction
  public String cloneCohort(
      String ogCohortId,
      String userEmail,
      String destinationStudyId,
      String displayName,
      String description) {
    // Cohort
    String clCohortId = RandomStringUtils.randomAlphanumeric(10);

    // underlay: same as that of the original rows
    MapSqlParameterSource cohortParamSets =
        new MapSqlParameterSource()
            .addValue("dst_study_id", destinationStudyId)
            .addValue("og_cohort_id", ogCohortId)
            .addValue("cl_cohort_id", clCohortId)
            .addValue("user_email", userEmail)
            .addValue("display_name", displayName)
            .addValue("description", description);

    String sql =
        "INSERT INTO cohort (study_id, id, underlay, created_by, last_modified_by, display_name, description, is_deleted) "
            + "(SELECT :dst_study_id, :cl_cohort_id, underlay, :user_email, :user_email, :display_name, :description, false "
            + "FROM cohort "
            + "WHERE id = :og_cohort_id)";
    JdbcUtils.insertRows(jdbcTemplate, "primary_entity_instance", sql, List.of(cohortParamSets));

    // Reviews
    List<Review> ogReviews = reviewDao.getAllReviews(ogCohortId, 0, Integer.MAX_VALUE);
    Map<String, String> ogRevisionIdReviewIdMap = new HashMap<>(); // revisionId -> reviewId
    Map<String, String> reviewIdMap = new HashMap<>(); // originalId -> clonedId
    List<MapSqlParameterSource> reviewParamSets = new ArrayList<>();
    List<MapSqlParameterSource> primaryEntityParamSets = new ArrayList<>();
    List<MapSqlParameterSource> annotValueParamSets = new ArrayList<>();

    ogReviews.forEach(
        review -> {
          String ogReviewId = review.getId();
          String clReviewId = RandomStringUtils.randomAlphanumeric(10);
          ogRevisionIdReviewIdMap.put(review.getRevision().getId(), ogReviewId);
          reviewIdMap.put(ogReviewId, clReviewId);

          reviewParamSets.add(
              ReviewDao.buildReviewParam(
                  clCohortId,
                  clReviewId,
                  review.getSize(),
                  review.getDisplayName(),
                  review.getDescription(),
                  userEmail));

          // id: same as that of the original rows
          primaryEntityParamSets.add(
              new MapSqlParameterSource()
                  .addValue("og_review_id", ogReviewId)
                  .addValue("cl_review_id", clReviewId));

          // annotation_key_id, primary_entity_instance_id: same as that of the original rows
          annotValueParamSets.add(
              new MapSqlParameterSource()
                  .addValue("og_cohort_id", ogCohortId)
                  .addValue("cl_cohort_id", clCohortId)
                  .addValue("og_review_id", ogReviewId)
                  .addValue("cl_review_id", clReviewId));
        });
    reviewDao.insertReviewRows(reviewParamSets); // fk: cohort(id)

    // Primary Entity Instance - fk: review(id)
    sql =
        "INSERT INTO primary_entity_instance (review_id, id, stable_index) "
            + "(SELECT :cl_review_id, id, stable_index "
            + "FROM primary_entity_instance "
            + "WHERE review_id = :og_review_id)";
    JdbcUtils.insertRows(jdbcTemplate, "primary_entity_instance", sql, primaryEntityParamSets);

    List<MapSqlParameterSource> revisionParamSets = new ArrayList<>();
    List<MapSqlParameterSource> criteriaParamSets = new ArrayList<>();

    List<Pair<String, CohortRevision.Builder>> ogRevisions =
        cohortDao.getRevisions(Set.of(ogCohortId), false);
    ogRevisions.forEach(
        builderPair -> {
          CohortRevision ogRevision = builderPair.getValue().build();
          String ogRevisionId = ogRevision.getId();
          String clRevisionId = RandomStringUtils.randomAlphanumeric(10);

          revisionParamSets.add(
              CohortDao.buildRevisionParam(
                  clCohortId,
                  clRevisionId,
                  ogRevision.getVersion(),
                  ogRevision.isMostRecent(),
                  ogRevision.isEditable(),
                  userEmail,
                  ogRevision.getRecordsCount(),
                  reviewIdMap.get(ogRevisionIdReviewIdMap.get(ogRevisionId))));

          criteriaParamSets.add(
              new MapSqlParameterSource()
                  .addValue("og_cohort_revision_id", ogRevisionId)
                  .addValue("cl_cohort_revision_id", clRevisionId));
        });

    // Cohort revision - fk: cohort(id), review(id)
    sql =
        "INSERT INTO cohort_revision (cohort_id, id, review_id, version, is_most_recent, is_editable, created_by, last_modified_by, records_count) "
            + "VALUES (:cohort_id, :id, :review_id, :version, :is_most_recent, :is_editable, :created_by, :last_modified_by, :records_count)";
    JdbcUtils.insertRows(jdbcTemplate, "cohort_revision", sql, revisionParamSets);

    // Criteria group section - fk: cohort_revision(id)
    sql =
        "INSERT INTO criteria_group_section (cohort_revision_id, id, display_name, operator, is_excluded, is_disabled, first_condition_reducing_operator, second_condition_reducing_operator, join_operator, join_operator_value, list_index) "
            + "(SELECT :cl_cohort_revision_id, id, display_name, operator, is_excluded, is_disabled, first_condition_reducing_operator, second_condition_reducing_operator, join_operator, join_operator_value, list_index "
            + "FROM criteria_group_section "
            + "WHERE cohort_revision_id = :og_cohort_revision_id)";
    JdbcUtils.insertRows(jdbcTemplate, "criteria_group_section", sql, criteriaParamSets);

    // Criteria group - fk: cohort_revision(id)
    sql =
        "INSERT INTO criteria_group (cohort_revision_id, criteria_group_section_id, id, display_name, condition_index, list_index, is_disabled) "
            + "(SELECT :cl_cohort_revision_id, criteria_group_section_id, id, display_name, condition_index, list_index, is_disabled "
            + "FROM criteria_group "
            + "WHERE cohort_revision_id = :og_cohort_revision_id)";
    JdbcUtils.insertRows(jdbcTemplate, "criteria_group", sql, criteriaParamSets);

    // Criteria - fk: cohort_revision(id)
    // concept_set_id, predefined_id: used in featureSet and not populated here
    sql =
        "INSERT INTO criteria (cohort_revision_id, criteria_group_section_id, criteria_group_id, id, display_name, plugin_name, plugin_version, selector_or_modifier_name, selection_data, ui_config, list_index) "
            + "(SELECT :cl_cohort_revision_id, criteria_group_section_id, criteria_group_id, id, display_name, plugin_name, plugin_version, selector_or_modifier_name, selection_data, ui_config, list_index "
            + "FROM criteria "
            + "WHERE cohort_revision_id = :og_cohort_revision_id)";
    JdbcUtils.insertRows(jdbcTemplate, "criteria", sql, criteriaParamSets);

    // Criteria tags - fk: concept_set(id)
    // concept_set_id: used in featureSet and not populated here
    sql =
        "INSERT INTO criteria_tag (cohort_revision_id, criteria_group_section_id, criteria_group_id, criteria_id, criteria_key, criteria_value) "
            + "(SELECT :cl_cohort_revision_id, criteria_group_section_id, criteria_group_id, criteria_id, criteria_key, criteria_value "
            + "FROM criteria_tag "
            + "WHERE cohort_revision_id = :og_cohort_revision_id)";
    JdbcUtils.insertRows(jdbcTemplate, "criteria_tag", sql, criteriaParamSets);

    List<MapSqlParameterSource> cohortIdParamSets =
        List.of(
            new MapSqlParameterSource()
                .addValue("og_cohort_id", ogCohortId)
                .addValue("cl_cohort_id", clCohortId));

    // Annotation Keys - fk: cohort(id)
    // id: same as that of the original rows
    sql =
        "INSERT INTO annotation_key (cohort_id, id, display_name, description, data_type) "
            + "(SELECT :cl_cohort_id, id, display_name, description, data_type "
            + "FROM annotation_key "
            + "WHERE cohort_id = :og_cohort_id)";
    int rowsAffected = JdbcUtils.insertRows(jdbcTemplate, "annotation_key", sql, cohortIdParamSets);

    // Annotation Key Enum Values - fk: annotation_key(id, cohortId)
    if (rowsAffected > 0) {
      sql =
          "INSERT INTO annotation_key_enum_value (cohort_id, annotation_key_id, enum) "
              + "(SELECT :cl_cohort_id, annotation_key_id, enum "
              + "FROM annotation_key_enum_value "
              + "WHERE cohort_id = :og_cohort_id)";
      JdbcUtils.insertRows(jdbcTemplate, "annotation_key_enum_value", sql, cohortIdParamSets);
    }

    // Annotation Values - fk: primary_entity_instance(id, reviewId), annotation_key(id, cohort_id))
    // annotation_key_id, primary_entity_instance_id: same id value in cloned rows
    sql =
        "INSERT INTO annotation_value (cohort_id, annotation_key_id, review_id, primary_entity_instance_id, bool_val, int64_val, string_val, date_val) "
            + "(SELECT :cl_cohort_id, annotation_key_id, :cl_review_id, primary_entity_instance_id, bool_val, int64_val, string_val, date_val "
            + "FROM annotation_value "
            + "WHERE cohort_id = :og_cohort_id AND review_id = :og_review_id)";
    JdbcUtils.insertRows(jdbcTemplate, "annotation_value", sql, annotValueParamSets);

    return clCohortId;
  }
}
