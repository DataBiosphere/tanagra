package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.artifact.Study;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
public class StudyServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(StudyServiceTest.class);

  @Autowired private StudyService studyService;

  @Test
  void createUpdateDelete() throws InterruptedException {
    // Create.
    String displayName = "study 1";
    String description = "first study";
    String createdByEmail = "abc@123.com";
    Study createdStudy =
        studyService.createStudy(
            Study.builder()
                .displayName(displayName)
                .description(description)
                .createdBy(createdByEmail));
    assertNotNull(createdStudy);
    LOGGER.info("Created study {} at {}", createdStudy.getStudyId(), createdStudy.getCreated());
    assertEquals(displayName, createdStudy.getDisplayName());
    assertEquals(description, createdStudy.getDescription());
    assertEquals(createdByEmail, createdStudy.getCreatedBy());
    assertEquals(createdStudy.getCreated(), createdStudy.getLastModified());

    // Update.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    String displayName2 = "study 1 updated";
    String description2 = "first study updated";
    Study updatedStudy =
        studyService.updateStudy(createdStudy.getStudyId(), displayName2, description2);
    assertNotNull(updatedStudy);
    LOGGER.info(
        "Updated study {} at {}", updatedStudy.getStudyId(), updatedStudy.getLastModified());
    assertEquals(displayName2, updatedStudy.getDisplayName());
    assertEquals(description2, updatedStudy.getDescription());
    assertTrue(updatedStudy.getLastModified().isAfter(updatedStudy.getCreated()));

    // Delete.
    studyService.deleteStudy(createdStudy.getStudyId());
    assertThrows(NotFoundException.class, () -> studyService.getStudy(createdStudy.getStudyId()));
  }

  @Test
  void listAllOrSelected() {
    // Create two studies.
    Study study1 =
        studyService.createStudy(Study.builder().displayName("study 1").createdBy("abc@123.com"));
    assertNotNull(study1);
    LOGGER.info("Created study {} at {}", study1.getStudyId(), study1.getCreated());
    Study study2 =
        studyService.createStudy(Study.builder().displayName("study 2").createdBy("abc@123.com"));
    assertNotNull(study2);
    LOGGER.info("Created study {} at {}", study2.getStudyId(), study2.getCreated());

    // List all.
    List<Study> allStudies = studyService.getAllStudies(0, 10);
    assertEquals(2, allStudies.size());

    // List selected.
    List<Study> selectedStudies = studyService.getStudies(List.of(study2.getStudyId()), 0, 10);
    assertEquals(1, selectedStudies.size());
    assertEquals(study2.getStudyId(), selectedStudies.get(0).getStudyId());
  }

  @Test
  void withProperties() throws InterruptedException {
    // Create without properties.
    Study study =
        studyService.createStudy(Study.builder().displayName("study 1").createdBy("abc@123.com"));
    assertNotNull(study);
    LOGGER.info("Created study {} at {}", study.getStudyId(), study.getCreated());

    // Add properties.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified timestamp differs.
    String key1 = "irb_number";
    String val1 = "123145";
    String key2 = "principal_investigator";
    String val2 = "george";
    Study updatedStudy1 =
        studyService.updateStudyProperties(study.getStudyId(), Map.of(key1, val1, key2, val2));
    assertEquals(2, updatedStudy1.getProperties().size());
    assertEquals(val1, updatedStudy1.getProperties().get(key1));
    assertEquals(val2, updatedStudy1.getProperties().get(key2));
    assertTrue(updatedStudy1.getLastModified().isAfter(updatedStudy1.getCreated()));

    // Delete properties.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified timestamp differs.
    Study updatedStudy2 = studyService.deleteStudyProperties(study.getStudyId(), List.of(key1));
    assertEquals(1, updatedStudy2.getProperties().size());
    assertEquals(val2, updatedStudy2.getProperties().get(key2));
    assertTrue(updatedStudy2.getLastModified().isAfter(updatedStudy1.getLastModified()));
  }
}
