package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_1;
import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_2;
import static bio.terra.tanagra.service.CriteriaValues.*;
import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.model.AnnotationKey;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.Study;
import java.util.List;
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
public class AnnotationServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationServiceTest.class);
  private static final String UNDERLAY_NAME = "cms_synpuf";

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
  @Autowired private AnnotationService annotationService;
  @Autowired private ReviewService reviewService;

  private Study study1;
  private Cohort cohort1, cohort2;

  @BeforeEach
  void createTwoCohorts() {
    String userEmail = "abc@123.com";

    study1 = studyService.createStudy(Study.builder().displayName("study 1"), userEmail);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    // Create cohort1 with criteria.
    cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("cohort 2")
                .description("first cohort"),
            userEmail,
            List.of(CRITERIA_GROUP_SECTION_1, CRITERIA_GROUP_SECTION_2));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    // Create cohort2 with criteria.
    cohort2 =
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
  }

  @AfterEach
  void deleteTwoCohorts() {
    try {
      studyService.deleteStudy(study1.getId());
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }
  }

  @Test
  void createUpdateDeleteKey() {
    // Create.
    String displayName = "annotation key 1";
    String description = "first annotation key";
    AnnotationKey createdAnnotationKey =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder()
                .displayName(displayName)
                .description(description)
                .dataType(Literal.DataType.INT64));
    assertNotNull(createdAnnotationKey);
    LOGGER.info("Created annotation key {}", createdAnnotationKey.getId());
    assertEquals(Literal.DataType.INT64, createdAnnotationKey.getDataType());
    assertEquals(displayName, createdAnnotationKey.getDisplayName());
    assertEquals(description, createdAnnotationKey.getDescription());

    // Update.
    String displayName2 = "annotation key 1 updated";
    String description2 = "first annotation key updated";
    AnnotationKey updatedAnnotationKey =
        annotationService.updateAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            createdAnnotationKey.getId(),
            displayName2,
            description2);
    assertNotNull(updatedAnnotationKey);
    LOGGER.info("Updated annotation key {}", updatedAnnotationKey.getId());
    assertEquals(displayName2, updatedAnnotationKey.getDisplayName());
    assertEquals(description2, updatedAnnotationKey.getDescription());

    // Delete.
    annotationService.deleteAnnotationKey(
        study1.getId(), cohort1.getId(), createdAnnotationKey.getId());
    assertThrows(
        NotFoundException.class,
        () ->
            annotationService.getAnnotationKey(
                study1.getId(), cohort1.getId(), createdAnnotationKey.getId()));
  }

  @Test
  void listAllOrSelectedKeys() {
    // Create one annotation key for cohort1.
    AnnotationKey annotationKey1 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder()
                .displayName("annotation key 1")
                .description("first annotation key")
                .dataType(Literal.DataType.BOOLEAN));
    assertNotNull(annotationKey1);
    LOGGER.info("Created annotation key {}", annotationKey1.getId());

    // Create two annotation keys for cohort2.
    AnnotationKey annotationKey2 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort2.getId(),
            AnnotationKey.builder()
                .displayName("annotation key 2")
                .description("second annotation key")
                .dataType(Literal.DataType.INT64));
    assertNotNull(annotationKey2);
    LOGGER.info("Created annotation key {}", annotationKey2.getId());
    AnnotationKey annotationKey3 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort2.getId(),
            AnnotationKey.builder()
                .displayName("annotation key 3")
                .description("third annotation key")
                .dataType(Literal.DataType.STRING)
                .enumVals(List.of("STATUS", "NOTES")));
    assertNotNull(annotationKey3);
    LOGGER.info("Created annotation key {}", annotationKey3.getId());

    // List all annotation keys for cohort2.
    List<AnnotationKey> allAnnotationKeys =
        annotationService.listAnnotationKeys(
            ResourceIdCollection.allResourceIds(), study1.getId(), cohort2.getId(), 0, 10);
    assertEquals(2, allAnnotationKeys.size());
    LOGGER.info(
        "Annotation keys found: {}, {}",
        allAnnotationKeys.get(0).getId(),
        allAnnotationKeys.get(1).getId());

    // List selected annotation key for cohort2.
    List<AnnotationKey> selectedAnnotationKeys =
        annotationService.listAnnotationKeys(
            ResourceIdCollection.forCollection(List.of(new ResourceId(annotationKey3.getId()))),
            study1.getId(),
            cohort2.getId(),
            0,
            10);
    assertEquals(1, selectedAnnotationKeys.size());
  }

  @Test
  void invalid() {
    // List all.
    List<AnnotationKey> allAnnotationKeys =
        annotationService.listAnnotationKeys(
            ResourceIdCollection.allResourceIds(), study1.getId(), cohort1.getId(), 0, 10);
    assertTrue(allAnnotationKeys.isEmpty());

    // List selected.
    List<AnnotationKey> selectedAnnotationKeys =
        annotationService.listAnnotationKeys(
            ResourceIdCollection.forCollection(List.of(new ResourceId("123"))),
            study1.getId(),
            cohort1.getId(),
            0,
            10);
    assertTrue(selectedAnnotationKeys.isEmpty());

    // Get invalid annotation key.
    assertThrows(
        NotFoundException.class, () -> annotationService.getAnnotationKey("789", "123", "456"));
    assertThrows(
        NotFoundException.class,
        () -> annotationService.getAnnotationKey(study1.getId(), cohort1.getId(), "123"));
  }

  @Test
  void keyWithEnumVals() {
    // Create.
    List<String> enumVals = List.of("STATUS", "NOTES");
    AnnotationKey createdAnnotationKey =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.STRING).enumVals(enumVals));
    assertNotNull(createdAnnotationKey);
    LOGGER.info("Created annotation key {}", createdAnnotationKey.getId());
    assertEquals(Literal.DataType.STRING, createdAnnotationKey.getDataType());
    assertEquals(enumVals.size(), createdAnnotationKey.getEnumVals().size());
    assertTrue(createdAnnotationKey.getEnumVals().containsAll(enumVals));

    // Update.
    String displayName2 = "annotation key 1 updated";
    String description2 = "first annotation key updated";
    AnnotationKey updatedAnnotationKey =
        annotationService.updateAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            createdAnnotationKey.getId(),
            displayName2,
            description2);
    assertNotNull(updatedAnnotationKey);
    LOGGER.info("Updated annotation key {}", updatedAnnotationKey.getId());
    assertEquals(displayName2, updatedAnnotationKey.getDisplayName());
    assertEquals(description2, updatedAnnotationKey.getDescription());

    // Delete.
    annotationService.deleteAnnotationKey(
        study1.getId(), cohort1.getId(), createdAnnotationKey.getId());
    assertThrows(
        NotFoundException.class,
        () ->
            annotationService.getAnnotationKey(
                study1.getId(), cohort1.getId(), createdAnnotationKey.getId()));
  }
}
