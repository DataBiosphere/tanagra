package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api.field.AttributeField;
import bio.terra.tanagra.api.field.ValueDisplayField;
import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.app.configuration.VersionConfiguration;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.inmemory.InMemoryRowResult;
import bio.terra.tanagra.service.artifact.ActivityLogService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.ActivityLog;
import bio.terra.tanagra.service.artifact.model.ActivityLogResource;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.export.DataExportService;
import bio.terra.tanagra.service.export.ExportRequest;
import bio.terra.tanagra.service.export.ExportResult;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
public class ActivityLogServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ActivityLogServiceTest.class);
  private static final String UNDERLAY_NAME = "cmssynpuf";

  private static final String USER_EMAIL_1 = "abc@123.com";
  private static final String USER_EMAIL_2 = "def@123.com";
  @Autowired private UnderlayService underlayService;
  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
  @Autowired private ReviewService reviewService;
  @Autowired private DataExportService dataExportService;
  @Autowired private ActivityLogService activityLogService;
  @Autowired private VersionConfiguration versionConfiguration;

  private Study study1;

  @BeforeEach
  void clearActivityLog() {
    activityLogService.clearAllActivityLogs();
  }

  @AfterEach
  void deleteStudy() {
    try {
      studyService.deleteStudy(study1.getId(), USER_EMAIL_1);
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }
  }

  @Test
  @Tag("requires-cloud-access")
  void createLogs() throws InterruptedException {
    // CREATE_STUDY
    study1 =
        studyService.createStudy(
            Study.builder().displayName("study 1").properties(Map.of("irb", "123")), USER_EMAIL_1);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    ActivityLogResource studyActivityLogResource =
        ActivityLogResource.builder()
            .type(ActivityLogResource.Type.STUDY)
            .studyId(study1.getId())
            .studyDisplayName(study1.getDisplayName())
            .studyProperties(Map.of("irb", "123"))
            .build();

    List<ActivityLog> activityLogs =
        activityLogService.listActivityLogs(0, 10, null, false, null, null);
    assertEquals(1, activityLogs.size());
    assertTrue(
        activityLogs
            .get(0)
            .isEquivalentTo(
                buildActivityLog(
                        USER_EMAIL_1, ActivityLog.Type.CREATE_STUDY, studyActivityLogResource)
                    .build()));

    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the activity log timestamp differs.

    // CREATE_COHORT
    Cohort cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder().underlay(UNDERLAY_NAME),
            USER_EMAIL_1,
            List.of(CRITERIA_GROUP_SECTION_3));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    ActivityLogResource.Builder cohortActivityLogResource =
        ActivityLogResource.builder()
            .type(ActivityLogResource.Type.COHORT)
            .studyId(study1.getId())
            .studyDisplayName(study1.getDisplayName())
            .studyProperties(Map.of("irb", "123"))
            .cohortId(cohort1.getId())
            .cohortDisplayName(cohort1.getDisplayName());

    activityLogs = activityLogService.listActivityLogs(0, 10, null, false, null, null);
    assertEquals(2, activityLogs.size());
    assertTrue(
        activityLogs
            .get(0)
            .isEquivalentTo(
                buildActivityLog(
                        USER_EMAIL_1,
                        ActivityLog.Type.CREATE_COHORT,
                        cohortActivityLogResource
                            .cohortRevisionId(cohort1.getMostRecentRevision().getId())
                            .build())
                    .build()));

    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the activity log timestamp differs.

    // CREATE_REVIEW
    ColumnHeaderSchema columnHeaderSchema =
        new ColumnHeaderSchema(List.of(new ColumnSchema("id", CellValue.SQLDataType.INT64)));
    QueryResult queryResult =
        new QueryResult(
            List.of(123L, 456L, 789L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    Review review1 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort1.getId(),
            Review.builder().displayName("review 1").description("first review").size(11),
            USER_EMAIL_2,
            queryResult,
            4_500_000L);
    assertNotNull(review1);
    LOGGER.info("Created review {} at {}", review1.getId(), review1.getCreated());

    ActivityLogResource reviewActivityLogResource =
        ActivityLogResource.builder()
            .type(ActivityLogResource.Type.REVIEW)
            .studyId(study1.getId())
            .studyDisplayName(study1.getDisplayName())
            .studyProperties(Map.of("irb", "123"))
            .cohortId(cohort1.getId())
            .cohortDisplayName(cohort1.getDisplayName())
            .reviewId(review1.getId())
            .reviewDisplayName(review1.getDisplayName())
            .cohortRevisionId(review1.getRevision().getId())
            .build();

    activityLogs = activityLogService.listActivityLogs(0, 10, null, false, null, null);
    assertEquals(3, activityLogs.size());
    assertTrue(
        activityLogs
            .get(0)
            .isEquivalentTo(
                buildActivityLog(
                        USER_EMAIL_2, ActivityLog.Type.CREATE_REVIEW, reviewActivityLogResource)
                    .recordsCount(4_500_000L)
                    .build()));

    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the activity log timestamp differs.

    // EXPORT_COHORT
    cohort1 =
        cohortService.getCohort(
            study1.getId(), cohort1.getId()); // Get the current cohort revision, post-review.
    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    Entity primaryEntity = underlay.getPrimaryEntity();
    // Select all attributes.
    List<ValueDisplayField> selectFields = new ArrayList<>();
    primaryEntity.getAttributes().stream()
        .forEach(
            attribute ->
                selectFields.add(
                    new AttributeField(underlay, primaryEntity, attribute, false, false)));
    ListQueryRequest listQueryRequest =
        new ListQueryRequest(underlay, primaryEntity, selectFields, null, null, 5, null, null);
    EntityFilter primaryEntityFilter =
        new AttributeFilter(
            underlay,
            primaryEntity,
            primaryEntity.getAttribute("year_of_birth"),
            BinaryFilterVariable.BinaryOperator.GREATER_THAN_OR_EQUAL,
            new Literal(1980L));
    String exportModel = "IPYNB_FILE_DOWNLOAD";
    ExportRequest.Builder exportRequest = ExportRequest.builder().model(exportModel);
    ExportResult exportResult =
        dataExportService.run(
            study1.getId(),
            List.of(cohort1.getId()),
            exportRequest,
            List.of(listQueryRequest),
            primaryEntityFilter,
            USER_EMAIL_2);
    assertNotNull(exportResult);

    activityLogs = activityLogService.listActivityLogs(0, 10, null, false, null, null);
    assertEquals(4, activityLogs.size());
    assertTrue(
        activityLogs
            .get(0)
            .isEquivalentTo(
                buildActivityLog(
                        USER_EMAIL_2,
                        ActivityLog.Type.EXPORT_COHORT,
                        cohortActivityLogResource
                            .cohortRevisionId(cohort1.getMostRecentRevision().getId())
                            .build())
                    .exportModel(exportModel)
                    .recordsCount(12_861L)
                    .build()));

    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the activity log timestamp differs.

    // DELETE_REVIEW
    reviewService.deleteReview(study1.getId(), cohort1.getId(), review1.getId(), USER_EMAIL_1);
    LOGGER.info("Deleted review {}", review1.getId());

    activityLogs = activityLogService.listActivityLogs(0, 10, null, false, null, null);
    assertEquals(5, activityLogs.size());
    assertTrue(
        activityLogs
            .get(0)
            .isEquivalentTo(
                buildActivityLog(
                        USER_EMAIL_1, ActivityLog.Type.DELETE_REVIEW, reviewActivityLogResource)
                    .recordsCount(4_500_000L)
                    .build()));

    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the activity log timestamp differs.

    // DELETE_COHORT
    cohort1 =
        cohortService.getCohort(
            study1.getId(), cohort1.getId()); // Get the current cohort revision, post-export.
    cohortService.deleteCohort(study1.getId(), cohort1.getId(), USER_EMAIL_2);
    LOGGER.info("Deleted cohort {}", cohort1.getId());

    activityLogs = activityLogService.listActivityLogs(0, 10, null, false, null, null);
    assertEquals(6, activityLogs.size());
    assertTrue(
        activityLogs
            .get(0)
            .isEquivalentTo(
                buildActivityLog(
                        USER_EMAIL_2,
                        ActivityLog.Type.DELETE_COHORT,
                        cohortActivityLogResource
                            .cohortRevisionId(cohort1.getMostRecentRevision().getId())
                            .build())
                    .build()));

    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the activity log timestamp differs.

    // DELETE_STUDY
    studyService.deleteStudy(study1.getId(), USER_EMAIL_2);
    LOGGER.info("Deleted study1 {}", study1.getId());

    activityLogs = activityLogService.listActivityLogs(0, 10, null, false, null, null);
    assertEquals(7, activityLogs.size());
    assertTrue(
        activityLogs
            .get(0)
            .isEquivalentTo(
                buildActivityLog(
                        USER_EMAIL_2, ActivityLog.Type.DELETE_STUDY, studyActivityLogResource)
                    .build()));
  }

  @Test
  void retrieveSingleLog() {
    // CREATE_STUDY
    study1 =
        studyService.createStudy(
            Study.builder().displayName("study 1").properties(Map.of("irb", "456")), USER_EMAIL_1);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    List<ActivityLog> activityLogs =
        activityLogService.listActivityLogs(0, 10, null, false, null, null);
    assertEquals(1, activityLogs.size());

    ActivityLog singleLog = activityLogService.getActivityLog(activityLogs.get(0).getId());
    assertNotNull(singleLog);
    assertEquals(activityLogs.get(0), singleLog);
  }

  @Test
  @SuppressWarnings("VariableDeclarationUsageDistance")
  void filterList() throws InterruptedException {
    // CREATE_STUDY
    study1 =
        studyService.createStudy(
            Study.builder().displayName("study 1").properties(Map.of("irb", "789")), USER_EMAIL_1);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the activity log timestamp differs.

    // CREATE_COHORT
    Cohort cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder().underlay(UNDERLAY_NAME),
            USER_EMAIL_1,
            List.of(CRITERIA_GROUP_SECTION_3));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the activity log timestamp differs.

    // DELETE_COHORT
    cohortService.deleteCohort(study1.getId(), cohort1.getId(), USER_EMAIL_2);
    LOGGER.info("Deleted cohort {}", cohort1.getId());

    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the activity log timestamp differs.

    // DELETE_STUDY
    studyService.deleteStudy(study1.getId(), USER_EMAIL_2);
    LOGGER.info("Deleted study1 {}", study1.getId());

    List<ActivityLog> allActivityLogs =
        activityLogService.listActivityLogs(0, 10, null, false, null, null);
    assertEquals(4, allActivityLogs.size());
    ActivityLog deleteStudyLog = allActivityLogs.get(0);
    ActivityLog deleteCohortLog = allActivityLogs.get(1);
    ActivityLog createCohortLog = allActivityLogs.get(2);
    ActivityLog createStudyLog = allActivityLogs.get(3);

    // Filter by user email, not exact match.
    List<ActivityLog> activityLogs =
        activityLogService.listActivityLogs(0, 10, "123.com", false, null, null);
    assertEquals(4, activityLogs.size());
    assertEquals(allActivityLogs, activityLogs);

    // Filter by user email, exact match.
    activityLogs = activityLogService.listActivityLogs(0, 10, USER_EMAIL_2, true, null, null);
    assertEquals(2, activityLogs.size());
    assertEquals(List.of(deleteStudyLog, deleteCohortLog), activityLogs);

    // Filter by activity type.
    activityLogs =
        activityLogService.listActivityLogs(
            0, 10, null, false, ActivityLog.Type.DELETE_COHORT, null);
    assertEquals(1, activityLogs.size());
    assertEquals(deleteCohortLog, activityLogs.get(0));

    // Filter by resource type.
    activityLogs =
        activityLogService.listActivityLogs(
            0, 10, null, false, null, ActivityLogResource.Type.STUDY);
    assertEquals(2, activityLogs.size());
    assertEquals(List.of(deleteStudyLog, createStudyLog), activityLogs);

    // Filter by user email, activity, and resource type.
    activityLogs =
        activityLogService.listActivityLogs(
            0,
            10,
            USER_EMAIL_1,
            true,
            ActivityLog.Type.CREATE_COHORT,
            ActivityLogResource.Type.COHORT);
    assertEquals(1, activityLogs.size());
    assertEquals(createCohortLog, activityLogs.get(0));

    // Filter by offset & limit.
    activityLogs = activityLogService.listActivityLogs(1, 2, null, false, null, null);
    assertEquals(2, activityLogs.size());
    assertEquals(List.of(deleteCohortLog, createCohortLog), activityLogs);
  }

  private ActivityLog.Builder buildActivityLog(
      String userEmail, ActivityLog.Type type, ActivityLogResource resource) {
    return ActivityLog.builder()
        .userEmail(userEmail)
        .versionGitTag(versionConfiguration.getGitTag())
        .versionGitHash(versionConfiguration.getGitHash())
        .versionBuild(versionConfiguration.getBuild())
        .type(type)
        .resources(List.of(resource));
  }
}
