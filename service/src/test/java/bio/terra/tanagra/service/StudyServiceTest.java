package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.model.Study;
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
            Study.builder().displayName(displayName).description(description), createdByEmail);
    assertNotNull(createdStudy);
    LOGGER.info("Created study {} at {}", createdStudy.getId(), createdStudy.getCreated());
    assertEquals(displayName, createdStudy.getDisplayName());
    assertEquals(description, createdStudy.getDescription());
    assertEquals(createdByEmail, createdStudy.getCreatedBy());
    assertEquals(createdByEmail, createdStudy.getLastModifiedBy());
    assertEquals(createdStudy.getCreated(), createdStudy.getLastModified());

    // Update.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    String displayName2 = "study 1 updated";
    String description2 = "first study updated";
    Study updatedStudy =
        studyService.updateStudy(createdStudy.getId(), "efg@123.com", displayName2, description2);
    assertNotNull(updatedStudy);
    LOGGER.info("Updated study {} at {}", updatedStudy.getId(), updatedStudy.getLastModified());
    assertEquals(displayName2, updatedStudy.getDisplayName());
    assertEquals(description2, updatedStudy.getDescription());
    assertTrue(updatedStudy.getLastModified().isAfter(updatedStudy.getCreated()));
    assertEquals("efg@123.com", updatedStudy.getLastModifiedBy());

    // Delete.
    studyService.deleteStudy(createdStudy.getId());
    assertThrows(NotFoundException.class, () -> studyService.getStudy(createdStudy.getId()));
  }

  @Test
  void listAllOrSelected() {
    // Create two studies.
    String createdByEmail = "abc@123.com";
    Study study1 = studyService.createStudy(Study.builder().displayName("study 1"), createdByEmail);
    assertNotNull(study1);
    LOGGER.info("Created study {} at {}", study1.getId(), study1.getCreated());
    Study study2 = studyService.createStudy(Study.builder().displayName("study 2"), createdByEmail);
    assertNotNull(study2);
    LOGGER.info("Created study {} at {}", study2.getId(), study2.getCreated());

    // List all.
    List<Study> allStudies = studyService.getAllStudies(0, 10);
    assertEquals(2, allStudies.size());

    // List selected.
    List<Study> selectedStudies = studyService.getStudies(List.of(study2.getId()), 0, 10);
    assertEquals(1, selectedStudies.size());
    assertEquals(study2.getId(), selectedStudies.get(0).getId());
  }

  @Test
  void withProperties() throws InterruptedException {
    String userEmail1 = "abc@123.com";
    String userEmail2 = "efg@123.com";

    // Create without properties.
    Study study = studyService.createStudy(Study.builder().displayName("study 1"), userEmail1);
    assertNotNull(study);
    LOGGER.info("Created study {} at {}", study.getId(), study.getCreated());

    // Add properties.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified timestamp differs.
    String key1 = "irb_number";
    String val1 = "123145";
    String key2 = "principal_investigator";
    String val2 = "george";
    Study updatedStudy1 =
        studyService.updateStudyProperties(
            study.getId(), userEmail2, Map.of(key1, val1, key2, val2));
    assertEquals(2, updatedStudy1.getProperties().size());
    assertEquals(val1, updatedStudy1.getProperties().get(key1));
    assertEquals(val2, updatedStudy1.getProperties().get(key2));
    assertTrue(updatedStudy1.getLastModified().isAfter(updatedStudy1.getCreated()));
    assertEquals(userEmail2, updatedStudy1.getLastModifiedBy());

    // Delete properties.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified timestamp differs.
    Study updatedStudy2 =
        studyService.deleteStudyProperties(study.getId(), userEmail1, List.of(key1));
    assertEquals(1, updatedStudy2.getProperties().size());
    assertEquals(val2, updatedStudy2.getProperties().get(key2));
    assertTrue(updatedStudy2.getLastModified().isAfter(updatedStudy1.getLastModified()));
    assertEquals(userEmail1, updatedStudy2.getLastModifiedBy());
  }
}
