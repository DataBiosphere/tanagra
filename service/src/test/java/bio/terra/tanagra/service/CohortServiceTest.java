package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_CONDITION_AND_DISABLED_DEMOGRAPHICS;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_PROCEDURE;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_TEMPORAL_DURING_SAME_ENCOUNTER;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_TEMPORAL_WITHIN_NUM_DAYS;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.DISABLED_CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.exception.*;
import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.AnnotationService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.AnnotationValue;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.CohortRevision;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
public class CohortServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(CohortServiceTest.class);
  private static final String UNDERLAY_NAME = "cmssynpuf";
  private static final String USER_EMAIL_1 = "abc@123.com";
  private static final String USER_EMAIL_2 = "def@123.com";

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
  @Autowired private ReviewService reviewService;
  @Autowired private AnnotationService annotationService;

  private Study study1;
  private Study study2;

  @BeforeEach
  void createTwoStudies() {
    study1 = studyService.createStudy(Study.builder().displayName("study 1"), USER_EMAIL_1);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    study2 = studyService.createStudy(Study.builder().displayName("study 2"), USER_EMAIL_2);
    assertNotNull(study2);
    LOGGER.info("Created study2 {} at {}", study2.getId(), study2.getCreated());
  }

  @AfterEach
  void deleteTwoStudies() {
    try {
      studyService.deleteStudy(study1.getId(), USER_EMAIL_1);
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }

    try {
      studyService.deleteStudy(study2.getId(), USER_EMAIL_1);
      LOGGER.info("Deleted study2 {}", study2.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study2", ex);
    }
  }

  @Test
  void createUpdateDelete() throws InterruptedException {
    // Create.
    String displayName = "cohort 1";
    String description = "first cohort";
    String createdByEmail = USER_EMAIL_1;
    Cohort createdCohort =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName(displayName)
                .description(description)
                .createdBy(createdByEmail));
    assertNotNull(createdCohort);
    LOGGER.info("Created cohort {} at {}", createdCohort.getId(), createdCohort.getCreated());
    assertEquals(UNDERLAY_NAME, createdCohort.getUnderlay());
    assertEquals(displayName, createdCohort.getDisplayName());
    assertEquals(description, createdCohort.getDescription());
    assertEquals(createdByEmail, createdCohort.getCreatedBy());
    assertEquals(createdByEmail, createdCohort.getLastModifiedBy());
    assertEquals(createdCohort.getCreated(), createdCohort.getLastModified());

    // Update.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    String displayName2 = "cohort 1 updated";
    String description2 = "first cohort updated";
    String updatedByEmail = "efg@123.com";
    Cohort updatedCohort =
        cohortService.updateCohort(
            study1.getId(),
            createdCohort.getId(),
            updatedByEmail,
            displayName2,
            description2,
            null);
    assertNotNull(updatedCohort);
    LOGGER.info("Updated cohort {} at {}", updatedCohort.getId(), updatedCohort.getLastModified());
    assertEquals(displayName2, updatedCohort.getDisplayName());
    assertEquals(description2, updatedCohort.getDescription());
    assertEquals(createdByEmail, updatedCohort.getCreatedBy());
    assertEquals(updatedByEmail, updatedCohort.getLastModifiedBy());
    assertTrue(updatedCohort.getLastModified().isAfter(updatedCohort.getCreated()));

    // Delete.
    cohortService.deleteCohort(study1.getId(), createdCohort.getId(), USER_EMAIL_1);
    List<Cohort> cohorts =
        cohortService.listCohorts(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.COHORT, ResourceId.forStudy(study1.getId())),
            0,
            10);
    assertFalse(cohorts.stream().map(Cohort::getId).toList().contains(createdCohort.getId()));
    Cohort cohort = cohortService.getCohort(study1.getId(), createdCohort.getId());
    assertTrue(cohort.isDeleted());
  }

  @Test
  void listAllOrSelected() {
    // Create one cohort in study1.
    Cohort cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 1")
                .description("first cohort")
                .createdBy(USER_EMAIL_1));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    // Create two cohorts in study2.
    Cohort cohort2 =
        cohortService.createCohort(
            study2.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 2")
                .description("second cohort")
                .createdBy(USER_EMAIL_1));
    assertNotNull(cohort2);
    LOGGER.info("Created cohort {} at {}", cohort2.getId(), cohort2.getCreated());
    Cohort cohort3 =
        cohortService.createCohort(
            study2.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 3")
                .description("third cohort")
                .createdBy(USER_EMAIL_1));
    assertNotNull(cohort3);
    LOGGER.info("Created cohort {} at {}", cohort3.getId(), cohort3.getCreated());

    // List all cohorts in study2.
    List<Cohort> allCohorts =
        cohortService.listCohorts(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.COHORT, ResourceId.forStudy(study2.getId())),
            0,
            10);
    assertEquals(2, allCohorts.size());
    LOGGER.info("cohorts found: {}, {}", allCohorts.get(0).getId(), allCohorts.get(1).getId());
    List<Cohort> allCohortsSortedByDisplayNameAsc =
        allCohorts.stream()
            .sorted(Comparator.comparing(Cohort::getDisplayName))
            .collect(Collectors.toList());
    assertEquals(allCohorts, allCohortsSortedByDisplayNameAsc);

    // List selected cohort in study2.
    List<Cohort> selectedCohorts =
        cohortService.listCohorts(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.COHORT),
                Set.of(ResourceId.forCohort(study2.getId(), cohort3.getId()))),
            0,
            10);
    assertEquals(1, selectedCohorts.size());
  }

  @Test
  void cloneCohort() {
    // Create first cohort with criteria, review, annotation keys and values
    Cohort cohort =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 1")
                .description("first cohort")
                .createdBy(USER_EMAIL_1),
            List.of(
                CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION,
                CRITERIA_GROUP_SECTION_PROCEDURE));
    assertNotNull(cohort);
    LOGGER.info("Created cohort {} at {}", cohort.getId(), cohort.getCreated());

    Review review1 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort.getId(),
            Review.builder().displayName("review 1").size(11),
            USER_EMAIL_1,
            List.of(10L, 11L, 12L),
            27);
    assertNotNull(review1);
    LOGGER.info("Created review {} at {}", review1.getId(), review1.getCreated());

    cohort = cohortService.getCohort(study1.getId(), cohort.getId());

    AnnotationKey annotationKey1 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort.getId(),
            AnnotationKey.builder().displayName("annotation key 1").dataType(DataType.BOOLEAN));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort.getId(),
        annotationKey1.getId(),
        review1.getId(),
        "11",
        List.of(Literal.forBoolean(true)));
    LOGGER.info(
        "Created annotation key {} with boolean value for cohort {}",
        annotationKey1.getId(),
        cohort.getId());

    AnnotationKey annotationKey2 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort.getId(),
            AnnotationKey.builder()
                .dataType(DataType.STRING)
                .enumVals(List.of("STATUS", "NOTES"))
                .displayName("annotation key 2"));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort.getId(),
        annotationKey2.getId(),
        review1.getId(),
        "12",
        List.of(Literal.forString("STATUS")));
    LOGGER.info(
        "Created annotation key {} with enum value for cohort {}",
        annotationKey2.getId(),
        cohort.getId());

    // Clone the cohort into the same study without new displayName and verify
    String newDescription = "cloned cohort description";
    Cohort clonedCohort =
        cohortService.cloneCohort(
            study1.getId(), cohort.getId(), USER_EMAIL_2, study1.getId(), null, newDescription);
    assertEquals(
        2,
        cohortService
            .listCohorts(
                ResourceCollection.allResourcesAllPermissions(
                    ResourceType.COHORT, ResourceId.forStudy(study1.getId())),
                0,
                10)
            .size());
    assertNotEquals(cohort.getId(), clonedCohort.getId());
    assertEquals(cohort.getUnderlay(), clonedCohort.getUnderlay());
    assertEquals("(Copy) " + cohort.getDisplayName(), clonedCohort.getDisplayName());
    assertEquals(newDescription, clonedCohort.getDescription());
    assertEquals(USER_EMAIL_2, clonedCohort.getCreatedBy());
    assertEquals(USER_EMAIL_2, clonedCohort.getLastModifiedBy());
    assertEquals(clonedCohort.getCreated(), clonedCohort.getLastModified());

    // compare most recent revision
    CohortRevision revisionVer1 = cohort.getMostRecentRevision();
    CohortRevision clonedRevisionVer1 = clonedCohort.getMostRecentRevision();
    assertNotEquals(revisionVer1.getId(), clonedRevisionVer1.getId());
    assertEquals(revisionVer1.getSections(), clonedRevisionVer1.getSections());
    assertEquals(revisionVer1.getVersion(), clonedRevisionVer1.getVersion());
    assertEquals(1, clonedRevisionVer1.getVersion());
    assertEquals(revisionVer1.isEditable(), clonedRevisionVer1.isEditable());
    assertTrue(clonedRevisionVer1.isEditable());
    assertEquals(revisionVer1.isMostRecent(), clonedRevisionVer1.isMostRecent());
    assertTrue(clonedRevisionVer1.isMostRecent());
    assertEquals(USER_EMAIL_2, clonedRevisionVer1.getCreatedBy());
    assertEquals(USER_EMAIL_2, clonedRevisionVer1.getLastModifiedBy());
    assertEquals(revisionVer1.getRecordsCount(), clonedRevisionVer1.getRecordsCount());

    // compare review
    List<Review> clonedReviews =
        reviewService.listReviews(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.REVIEW, ResourceId.forCohort(study1.getId(), clonedCohort.getId())),
            0,
            10);
    assertEquals(1, clonedReviews.size());

    Review clonedReview1 = clonedReviews.get(0);
    assertNotEquals(review1.getId(), clonedReview1.getId());
    assertEquals(review1.getDisplayName(), clonedReview1.getDisplayName());
    assertEquals(review1.getDescription(), clonedReview1.getDescription());
    assertEquals(review1.getSize(), clonedReview1.getSize());
    assertEquals(USER_EMAIL_2, clonedReview1.getCreatedBy());
    assertEquals(USER_EMAIL_2, clonedReview1.getLastModifiedBy());
    assertEquals(review1.isDeleted(), clonedReview1.isDeleted());

    // compare the review's revision (should be the initial revision)
    CohortRevision revisionVer0 = review1.getRevision();
    CohortRevision clonedRevisionVer0 = clonedReview1.getRevision();
    assertNotEquals(revisionVer0.getId(), clonedRevisionVer0.getId());
    assertEquals(revisionVer0.getSections(), clonedRevisionVer0.getSections());
    assertEquals(revisionVer0.getVersion(), clonedRevisionVer0.getVersion());
    assertEquals(0, clonedRevisionVer0.getVersion());
    assertEquals(revisionVer0.isEditable(), clonedRevisionVer0.isEditable());
    assertFalse(clonedRevisionVer0.isEditable());
    assertEquals(revisionVer0.isMostRecent(), clonedRevisionVer0.isMostRecent());
    assertFalse(clonedRevisionVer0.isMostRecent());
    assertEquals(USER_EMAIL_2, clonedRevisionVer0.getCreatedBy());
    assertEquals(USER_EMAIL_2, clonedRevisionVer0.getLastModifiedBy());
    assertEquals(revisionVer0.getRecordsCount(), clonedRevisionVer0.getRecordsCount());

    // Compare annotation values (includes keys)
    List<AnnotationValue> annotValues =
        annotationService.getAllAnnotationValues(study1.getId(), cohort.getId()).stream()
            .map(AnnotationValue.Builder::build)
            .toList();
    List<AnnotationValue> clonedAnnotValues =
        annotationService.getAllAnnotationValues(study1.getId(), clonedCohort.getId()).stream()
            .map(AnnotationValue.Builder::build)
            .toList();
    assertEquals(annotValues, clonedAnnotValues);

    // Clone the cohort into different study without new description and verify
    String newDisplayName = "cloned cohort displayName";
    Cohort anotherCohort =
        cohortService.cloneCohort(
            study1.getId(), cohort.getId(), USER_EMAIL_2, study2.getId(), newDisplayName, null);
    assertEquals(
        2,
        cohortService
            .listCohorts(
                ResourceCollection.allResourcesAllPermissions(
                    ResourceType.COHORT, ResourceId.forStudy(study1.getId())),
                0,
                10)
            .size());
    List<Cohort> study2Cohorts =
        cohortService.listCohorts(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.COHORT, ResourceId.forStudy(study2.getId())),
            0,
            10);
    assertEquals(1, study2Cohorts.size());
    assertEquals(anotherCohort.getId(), study2Cohorts.get(0).getId());
    assertEquals(newDisplayName, anotherCohort.getDisplayName());
    assertEquals(cohort.getDescription(), anotherCohort.getDescription());
  }

  @Test
  void invalid() {
    // List all.
    List<Cohort> allCohorts =
        cohortService.listCohorts(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.COHORT, ResourceId.forStudy(study1.getId())),
            0,
            10);
    assertTrue(allCohorts.isEmpty());

    // List selected.
    List<Cohort> selectedCohorts =
        cohortService.listCohorts(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.COHORT),
                Set.of(ResourceId.forCohort(study1.getId(), "123"))),
            0,
            10);
    assertTrue(selectedCohorts.isEmpty());

    // Get invalid cohort.
    assertThrows(NotFoundException.class, () -> cohortService.getCohort("789", "123"));
    assertThrows(NotFoundException.class, () -> cohortService.getCohort(study1.getId(), "123"));

    // Specify invalid underlay.
    assertThrows(
        NotFoundException.class,
        () ->
            cohortService.createCohort(
                study1.getId(),
                Cohort.builder().underlay("invalid_underlay").createdBy(USER_EMAIL_1)));

    // Display name length exceeds maximum.
    assertThrows(
        BadRequestException.class,
        () ->
            cohortService.createCohort(
                study1.getId(),
                Cohort.builder()
                    .underlay(UNDERLAY_NAME)
                    .displayName("123456789012345678901234567890123456789012345678901")
                    .createdBy(USER_EMAIL_1)));
  }

  @Test
  void withCriteria() throws InterruptedException {
    // Create cohort1 without criteria.
    Cohort cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 1")
                .description("first cohort")
                .createdBy(USER_EMAIL_1));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    // Update cohort1 to add criteria.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    Cohort updatedCohort1 =
        cohortService.updateCohort(
            study1.getId(),
            cohort1.getId(),
            USER_EMAIL_1,
            null,
            null,
            List.of(
                CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION,
                CRITERIA_GROUP_SECTION_PROCEDURE));
    assertNotNull(updatedCohort1);
    LOGGER.info(
        "Updated cohort {} at {}", updatedCohort1.getId(), updatedCohort1.getLastModified());
    assertTrue(updatedCohort1.getLastModified().isAfter(updatedCohort1.getCreated()));
    assertEquals(2, updatedCohort1.getMostRecentRevision().getSections().size());
    assertEquals(
        List.of(
            CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION, CRITERIA_GROUP_SECTION_PROCEDURE),
        updatedCohort1.getMostRecentRevision().getSections());

    // Update cohort1 with disabled criteria group section and disabled criteria group.
    updatedCohort1 =
        cohortService.updateCohort(
            study1.getId(),
            cohort1.getId(),
            USER_EMAIL_1,
            null,
            null,
            List.of(
                DISABLED_CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION,
                CRITERIA_GROUP_SECTION_CONDITION_AND_DISABLED_DEMOGRAPHICS));
    assertNotNull(updatedCohort1);
    LOGGER.info(
        "Updated cohort {} at {}", updatedCohort1.getId(), updatedCohort1.getLastModified());
    assertEquals(2, updatedCohort1.getMostRecentRevision().getSections().size());
    assertEquals(
        List.of(
            DISABLED_CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION,
            CRITERIA_GROUP_SECTION_CONDITION_AND_DISABLED_DEMOGRAPHICS),
        updatedCohort1.getMostRecentRevision().getSections());

    // Create cohort2 with criteria.
    Cohort cohort2 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 2")
                .description("second cohort")
                .createdBy(USER_EMAIL_1),
            List.of(CRITERIA_GROUP_SECTION_PROCEDURE));
    assertNotNull(cohort2);
    LOGGER.info("Created cohort {} at {}", cohort2.getId(), cohort2.getCreated());
    assertEquals(1, cohort2.getMostRecentRevision().getSections().size());
    assertEquals(
        List.of(CRITERIA_GROUP_SECTION_PROCEDURE), cohort2.getMostRecentRevision().getSections());

    // Update cohort2 criteria only.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    Cohort updatedCohort2 =
        cohortService.updateCohort(
            study1.getId(),
            cohort2.getId(),
            USER_EMAIL_1,
            null,
            null,
            List.of(CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION));
    assertNotNull(updatedCohort2);
    LOGGER.info(
        "Updated cohort {} at {}", updatedCohort2.getId(), updatedCohort2.getLastModified());
    assertTrue(updatedCohort2.getLastModified().isAfter(updatedCohort2.getCreated()));
    assertEquals(1, updatedCohort2.getMostRecentRevision().getSections().size());
    assertEquals(
        List.of(CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION),
        updatedCohort2.getMostRecentRevision().getSections());

    // Create cohort3 with temporal criteria group sections.
    Cohort cohort3 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 3")
                .description("third cohort")
                .createdBy(USER_EMAIL_1),
            List.of(CRITERIA_GROUP_SECTION_TEMPORAL_WITHIN_NUM_DAYS));
    assertNotNull(cohort3);
    LOGGER.info("Created cohort {} at {}", cohort3.getId(), cohort3.getCreated());
    assertEquals(1, cohort3.getMostRecentRevision().getSections().size());
    assertEquals(
        List.of(CRITERIA_GROUP_SECTION_TEMPORAL_WITHIN_NUM_DAYS),
        cohort3.getMostRecentRevision().getSections());

    // Update cohort3 criteria only.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    Cohort updatedCohort3 =
        cohortService.updateCohort(
            study1.getId(),
            cohort3.getId(),
            USER_EMAIL_1,
            null,
            null,
            List.of(CRITERIA_GROUP_SECTION_TEMPORAL_DURING_SAME_ENCOUNTER));
    assertNotNull(updatedCohort3);
    LOGGER.info(
        "Updated cohort {} at {}", updatedCohort3.getId(), updatedCohort3.getLastModified());
    assertTrue(updatedCohort3.getLastModified().isAfter(updatedCohort3.getCreated()));
    assertEquals(1, updatedCohort3.getMostRecentRevision().getSections().size());
    assertEquals(
        List.of(CRITERIA_GROUP_SECTION_TEMPORAL_DURING_SAME_ENCOUNTER),
        updatedCohort3.getMostRecentRevision().getSections());
  }
}
