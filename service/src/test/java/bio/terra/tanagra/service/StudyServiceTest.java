package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.Study;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
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

  @AfterEach
  void deleteAll() {
    List<Study> allStudies =
        studyService.listStudies(
            ResourceCollection.allResourcesAllPermissions(ResourceType.STUDY), 0, 100);
    for (Study study : allStudies) {
      try {
        studyService.deleteStudy(study.getId());
      } catch (Exception ex) {
        LOGGER.error("Error deleting study", ex);
      }
    }
  }

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
    Study study1 =
        studyService.createStudy(
            Study.builder()
                .displayName("study 1")
                .description("oneoneone")
                .properties(Map.of("irb", "123")),
            createdByEmail);
    assertNotNull(study1);
    assertEquals(1, study1.getProperties().size());
    assertEquals("123", study1.getProperties().get("irb"));
    LOGGER.info("Created study {} at {}", study1.getId(), study1.getCreated());
    Study study2 =
        studyService.createStudy(
            Study.builder()
                .displayName("study 2")
                .description("twotwotwo")
                .properties(Map.of("irb", "456")),
            createdByEmail);
    assertNotNull(study2);
    assertEquals(1, study2.getProperties().size());
    assertEquals("456", study2.getProperties().get("irb"));
    LOGGER.info("Created study {} at {}", study2.getId(), study2.getCreated());

    // List all.
    List<Study> allStudies =
        studyService.listStudies(
            ResourceCollection.allResourcesAllPermissions(ResourceType.STUDY), 0, 10);
    assertEquals(2, allStudies.size());

    // List selected.
    List<Study> selectedStudies =
        studyService.listStudies(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.STUDY),
                Set.of(ResourceId.forStudy(study2.getId()))),
            0,
            10);
    assertEquals(1, selectedStudies.size());
    assertEquals(study2.getId(), selectedStudies.get(0).getId());

    // List all with filter.
    Study.Builder filter1 =
        Study.builder().displayName("1").description("one").properties(Map.of("irb", "23"));
    List<Study> allStudiesWithFilter =
        studyService.listStudies(
            ResourceCollection.allResourcesAllPermissions(ResourceType.STUDY), 0, 10, filter1);
    assertEquals(1, allStudiesWithFilter.size());
    assertEquals(study1.getId(), allStudiesWithFilter.get(0).getId());

    // List selected with filter.
    Study.Builder filter2 = Study.builder().properties(Map.of("irb", "45"));
    List<Study> selectedStudiesWithFilter =
        studyService.listStudies(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.STUDY),
                Set.of(ResourceId.forStudy(study1.getId()))),
            0,
            10,
            filter2);
    assertTrue(selectedStudiesWithFilter.isEmpty());
  }

  @Test
  void invalid() {
    // List all.
    List<Study> allStudies =
        studyService.listStudies(
            ResourceCollection.allResourcesAllPermissions(ResourceType.STUDY), 0, 10);
    assertTrue(allStudies.isEmpty());

    // List selected.
    List<Study> selectedStudies =
        studyService.listStudies(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.STUDY), Set.of(ResourceId.forStudy("123"))),
            0,
            10);
    assertTrue(selectedStudies.isEmpty());

    // Get invalid.
    assertThrows(NotFoundException.class, () -> studyService.getStudy("123"));
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
