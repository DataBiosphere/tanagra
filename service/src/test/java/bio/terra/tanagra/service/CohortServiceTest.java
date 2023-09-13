package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_1;
import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_2;
import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.NotFoundException;
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
  private static final String UNDERLAY_NAME = "cms_synpuf";

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;

  private Study study1;
  private Study study2;

  @BeforeEach
  void createTwoStudies() {
    study1 = studyService.createStudy(Study.builder().displayName("study 1"), "abc@123.com");
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    study2 = studyService.createStudy(Study.builder().displayName("study 2"), "def@123.com");
    assertNotNull(study2);
    LOGGER.info("Created study2 {} at {}", study2.getId(), study2.getCreated());
  }

  @AfterEach
  void deleteTwoStudies() {
    try {
      studyService.deleteStudy(study1.getId(), "abc@123.com");
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }

    try {
      studyService.deleteStudy(study2.getId(), "abc@123.com");
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
    String createdByEmail = "abc@123.com";
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
    cohortService.deleteCohort(study1.getId(), createdCohort.getId(), "abc@123.com");
    List<Cohort> cohorts =
        cohortService.listCohorts(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.COHORT, ResourceId.forStudy(study1.getId())),
            0,
            10);
    assertFalse(
        cohorts.stream()
            .map(Cohort::getId)
            .collect(Collectors.toList())
            .contains(createdCohort.getId()));
    Cohort cohort = cohortService.getCohort(study1.getId(), createdCohort.getId());
    assertTrue(cohort.isDeleted());
  }

  @Test
  void listAllOrSelected() {
    String userEmail = "abc@123.com";

    // Create one cohort in study1.
    Cohort cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 1")
                .description("first cohort"),
            userEmail);
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
            userEmail);
    assertNotNull(cohort2);
    LOGGER.info("Created cohort {} at {}", cohort2.getId(), cohort2.getCreated());
    Cohort cohort3 =
        cohortService.createCohort(
            study2.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 3")
                .description("third cohort"),
            userEmail);
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
                study1.getId(), Cohort.builder().underlay("invalid_underlay"), "abc@123.com"));
  }

  @Test
  void withCriteria() throws InterruptedException {
    String userEmail = "abc@123.com";

    // Create cohort1 without criteria.
    Cohort cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 1")
                .description("first cohort"),
            userEmail);
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    // Update cohort1 to add criteria.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    Cohort updatedCohort1 =
        cohortService.updateCohort(
            study1.getId(),
            cohort1.getId(),
            userEmail,
            null,
            null,
            List.of(CRITERIA_GROUP_SECTION_1, CRITERIA_GROUP_SECTION_2));
    assertNotNull(updatedCohort1);
    LOGGER.info(
        "Updated cohort {} at {}", updatedCohort1.getId(), updatedCohort1.getLastModified());
    assertTrue(updatedCohort1.getLastModified().isAfter(updatedCohort1.getCreated()));
    assertEquals(2, updatedCohort1.getMostRecentRevision().getSections().size());
    assertEquals(
        List.of(CRITERIA_GROUP_SECTION_1, CRITERIA_GROUP_SECTION_2),
        updatedCohort1.getMostRecentRevision().getSections());

    // Create cohort2 with criteria.
    Cohort cohort2 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 2")
                .description("second cohort"),
            userEmail,
            List.of(CRITERIA_GROUP_SECTION_2));
    assertNotNull(cohort2);
    LOGGER.info("Created cohort {} at {}", cohort2.getId(), cohort2.getCreated());
    assertEquals(1, cohort2.getMostRecentRevision().getSections().size());
    assertEquals(List.of(CRITERIA_GROUP_SECTION_2), cohort2.getMostRecentRevision().getSections());

    // Update cohort2 criteria only.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    Cohort updatedCohort2 =
        cohortService.updateCohort(
            study1.getId(),
            cohort1.getId(),
            userEmail,
            null,
            null,
            List.of(CRITERIA_GROUP_SECTION_1));
    assertNotNull(updatedCohort2);
    LOGGER.info(
        "Updated cohort {} at {}", updatedCohort2.getId(), updatedCohort2.getLastModified());
    assertTrue(updatedCohort2.getLastModified().isAfter(updatedCohort2.getCreated()));
    assertEquals(1, updatedCohort2.getMostRecentRevision().getSections().size());
    assertEquals(
        List.of(CRITERIA_GROUP_SECTION_1), updatedCohort2.getMostRecentRevision().getSections());
  }
}
