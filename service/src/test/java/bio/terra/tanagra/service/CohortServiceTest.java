package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_DEMOGRAPHICS_AND_CONDITION;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_PROCEDURE;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_TEMPORAL_DURING_SAME_ENCOUNTER;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_TEMPORAL_WITHIN_NUM_DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.exception.*;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
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
                .description(description),
            createdByEmail);
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
    assertFalse(
        cohorts.stream()
            .map(Cohort::getId)
            .toList()
            .contains(createdCohort.getId()));
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
                .description("first cohort"),
            USER_EMAIL_1);
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    // Create two cohorts in study2.
    Cohort cohort2 =
        cohortService.createCohort(
            study2.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 2")
                .description("second cohort"),
            USER_EMAIL_1);
    assertNotNull(cohort2);
    LOGGER.info("Created cohort {} at {}", cohort2.getId(), cohort2.getCreated());
    Cohort cohort3 =
        cohortService.createCohort(
            study2.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 3")
                .description("third cohort"),
            USER_EMAIL_1);
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
                study1.getId(), Cohort.builder().underlay("invalid_underlay"), USER_EMAIL_1));

    // Display name length exceeds maximum.
    assertThrows(
        BadRequestException.class,
        () ->
            cohortService.createCohort(
                study1.getId(),
                Cohort.builder()
                    .underlay(UNDERLAY_NAME)
                    .displayName("123456789012345678901234567890123456789012345678901"),
                USER_EMAIL_1));
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
                .description("first cohort"),
            USER_EMAIL_1);
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

    // Create cohort2 with criteria.
    Cohort cohort2 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 2")
                .description("second cohort"),
            USER_EMAIL_1,
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
                .description("third cohort"),
            USER_EMAIL_1,
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
