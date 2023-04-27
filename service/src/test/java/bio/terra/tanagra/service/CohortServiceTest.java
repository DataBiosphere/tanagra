package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.Study;
import java.util.concurrent.TimeUnit;
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

  private Study study1, study2;

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
      studyService.deleteStudy(study1.getId());
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }

    try {
      studyService.deleteStudy(study2.getId());
      LOGGER.info("Deleted study2 {}", study2.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study2", ex);
    }
  }

  @Test
  void createUpdateDelete() throws InterruptedException {
    // Create.
    String underlayName = "cms_synpuf";
    String displayName = "cohort 1";
    String description = "first cohort";
    String createdByEmail = "abc@123.com";
    Cohort createdCohort =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlayName(underlayName)
                .displayName(displayName)
                .description(description)
                .createdBy(createdByEmail)
                .lastModifiedBy(createdByEmail));
    assertNotNull(createdCohort);
    LOGGER.info("Created cohort {} at {}", createdCohort.getId(), createdCohort.getCreated());
    assertEquals(underlayName, createdCohort.getUnderlayName());
    assertEquals(displayName, createdCohort.getDisplayName());
    assertEquals(description, createdCohort.getDescription());
    assertEquals(createdByEmail, createdCohort.getCreatedBy());
    assertEquals(createdByEmail, createdCohort.getLastModifiedBy());
    assertEquals(createdCohort.getCreated(), createdCohort.getLastModified());

    // Update.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    String displayName2 = "cohort 1 updated";
    String description2 = "first cohort updated";
    Cohort updatedCohort =
        cohortService.updateCohort(
            study1.getId(),
            createdCohort.getId(),
            createdByEmail,
            displayName2,
            description2,
            null);
    assertNotNull(updatedCohort);
    LOGGER.info("Updated cohort {} at {}", updatedCohort.getId(), updatedCohort.getLastModified());
    assertEquals(displayName2, updatedCohort.getDisplayName());
    assertEquals(description2, updatedCohort.getDescription());
    assertTrue(updatedCohort.getLastModified().isAfter(updatedCohort.getCreated()));

    // Delete.
    cohortService.deleteCohort(study1.getId(), createdCohort.getId());
    assertThrows(
        NotFoundException.class,
        () -> cohortService.getCohort(study1.getId(), createdCohort.getId()));
  }

  @Test
  void listAllOrSelected() {
    // Create two studies.
    //    Study study1 =
    //        studyService.createStudy(Study.builder().displayName("study
    // 1").createdBy("abc@123.com"));
    //    assertNotNull(study1);
    //    LOGGER.info("Created study {} at {}", study1.getId(), study1.getCreated());
    //    Study study2 =
    //        studyService.createStudy(Study.builder().displayName("study
    // 2").createdBy("abc@123.com"));
    //    assertNotNull(study2);
    //    LOGGER.info("Created study {} at {}", study2.getId(), study2.getCreated());
    //
    //    // List all.
    //    List<Study> allStudies = studyService.getAllStudies(0, 10);
    //    assertEquals(2, allStudies.size());
    //
    //    // List selected.
    //    List<Study> selectedStudies = studyService.getStudies(List.of(study2.getId()), 0, 10);
    //    assertEquals(1, selectedStudies.size());
    //    assertEquals(study2.getId(), selectedStudies.get(0).getId());
  }
}
