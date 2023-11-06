package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaValues.CONDITION_EQ_DIABETES;
import static bio.terra.tanagra.service.CriteriaValues.ETHNICITY_EQ_JAPANESE;
import static bio.terra.tanagra.service.CriteriaValues.GENDER_EQ_WOMAN;
import static bio.terra.tanagra.service.CriteriaValues.PROCEDURE_EQ_AMPUTATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.ConceptSetService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
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
public class ConceptSetServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConceptSetServiceTest.class);
  private static final String UNDERLAY_NAME = "cms_synpuf";

  @Autowired private StudyService studyService;
  @Autowired private ConceptSetService conceptSetService;

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
    String displayName = "concept set 1";
    String description = "first concept set";
    String createdByEmail = "abc@123.com";
    ConceptSet createdConceptSet =
        conceptSetService.createConceptSet(
            study1.getId(),
            ConceptSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName(displayName)
                .description(description)
                .entity(GENDER_EQ_WOMAN.getKey())
                .criteria(List.of(GENDER_EQ_WOMAN.getValue())),
            createdByEmail);
    assertNotNull(createdConceptSet);
    LOGGER.info(
        "Created concept set {} at {}", createdConceptSet.getId(), createdConceptSet.getCreated());
    assertEquals(UNDERLAY_NAME, createdConceptSet.getUnderlay());
    assertEquals(displayName, createdConceptSet.getDisplayName());
    assertEquals(description, createdConceptSet.getDescription());
    assertEquals(createdByEmail, createdConceptSet.getCreatedBy());
    assertEquals(createdByEmail, createdConceptSet.getLastModifiedBy());
    assertEquals(createdConceptSet.getCreated(), createdConceptSet.getLastModified());
    assertEquals(GENDER_EQ_WOMAN.getKey(), createdConceptSet.getEntity());
    assertEquals(1, createdConceptSet.getCriteria().size());
    assertTrue(createdConceptSet.getCriteria().contains(GENDER_EQ_WOMAN.getValue()));

    // Update.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    String displayName2 = "concept set 1 updated";
    String description2 = "first concept set updated";
    String updatedByEmail = "efg@123.com";
    ConceptSet updatedConceptSet =
        conceptSetService.updateConceptSet(
            study1.getId(),
            createdConceptSet.getId(),
            updatedByEmail,
            displayName2,
            description2,
            CONDITION_EQ_DIABETES.getKey(),
            List.of(CONDITION_EQ_DIABETES.getValue()));
    assertNotNull(updatedConceptSet);
    LOGGER.info(
        "Updated concept set {} at {}",
        updatedConceptSet.getId(),
        updatedConceptSet.getLastModified());
    assertEquals(displayName2, updatedConceptSet.getDisplayName());
    assertEquals(description2, updatedConceptSet.getDescription());
    assertEquals(createdByEmail, updatedConceptSet.getCreatedBy());
    assertEquals(updatedByEmail, updatedConceptSet.getLastModifiedBy());
    assertTrue(updatedConceptSet.getLastModified().isAfter(updatedConceptSet.getCreated()));
    assertEquals(CONDITION_EQ_DIABETES.getKey(), updatedConceptSet.getEntity());
    assertEquals(1, updatedConceptSet.getCriteria().size());
    assertTrue(updatedConceptSet.getCriteria().contains(CONDITION_EQ_DIABETES.getValue()));

    // Delete.
    conceptSetService.deleteConceptSet(study1.getId(), createdConceptSet.getId());
    List<ConceptSet> conceptSets =
        conceptSetService.listConceptSets(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.CONCEPT_SET, ResourceId.forStudy(study1.getId())),
            0,
            10);
    assertFalse(
        conceptSets.stream()
            .map(ConceptSet::getId)
            .collect(Collectors.toList())
            .contains(createdConceptSet.getId()));
    ConceptSet conceptSet =
        conceptSetService.getConceptSet(study1.getId(), createdConceptSet.getId());
    assertTrue(conceptSet.isDeleted());
  }

  @Test
  void listAllOrSelected() {
    String userEmail = "abc@123.com";

    // Create one concept set in study1.
    ConceptSet conceptSet1 =
        conceptSetService.createConceptSet(
            study1.getId(),
            ConceptSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("concept set 1")
                .description("first concept set")
                .entity(ETHNICITY_EQ_JAPANESE.getKey())
                .criteria(List.of(ETHNICITY_EQ_JAPANESE.getValue())),
            userEmail);
    assertNotNull(conceptSet1);
    LOGGER.info("Created concept set {} at {}", conceptSet1.getId(), conceptSet1.getCreated());

    // Create two concept sets in study2.
    ConceptSet conceptSet2 =
        conceptSetService.createConceptSet(
            study2.getId(),
            ConceptSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("concept set 2")
                .description("second concept set")
                .entity(PROCEDURE_EQ_AMPUTATION.getKey())
                .criteria(List.of(PROCEDURE_EQ_AMPUTATION.getValue())),
            userEmail);
    assertNotNull(conceptSet2);
    LOGGER.info("Created concept set {} at {}", conceptSet2.getId(), conceptSet2.getCreated());
    ConceptSet conceptSet3 =
        conceptSetService.createConceptSet(
            study2.getId(),
            ConceptSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("concept set 3")
                .description("third concept set")
                .entity(GENDER_EQ_WOMAN.getKey())
                .criteria(List.of(GENDER_EQ_WOMAN.getValue())),
            userEmail);
    assertNotNull(conceptSet3);
    LOGGER.info("Created concept set {} at {}", conceptSet3.getId(), conceptSet3.getCreated());

    // List all cohorts in study2.
    List<ConceptSet> allConceptSets =
        conceptSetService.listConceptSets(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.CONCEPT_SET, ResourceId.forStudy(study2.getId())),
            0,
            10);
    assertEquals(2, allConceptSets.size());
    LOGGER.info(
        "concept sets found: {}, {}", allConceptSets.get(0).getId(), allConceptSets.get(1).getId());
    List<ConceptSet> allConceptSetsSortedByDisplayNameAsc =
        allConceptSets.stream()
            .sorted(Comparator.comparing(ConceptSet::getDisplayName))
            .collect(Collectors.toList());
    assertEquals(allConceptSets, allConceptSetsSortedByDisplayNameAsc);

    // List selected concept set in study2.
    List<ConceptSet> selectedConceptSets =
        conceptSetService.listConceptSets(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.CONCEPT_SET),
                Set.of(ResourceId.forConceptSet(study2.getId(), conceptSet3.getId()))),
            0,
            10);
    assertEquals(1, selectedConceptSets.size());
  }

  @Test
  void invalid() {
    // List all.
    List<ConceptSet> allConceptSets =
        conceptSetService.listConceptSets(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.CONCEPT_SET, ResourceId.forStudy(study1.getId())),
            0,
            10);
    assertTrue(allConceptSets.isEmpty());

    // List selected.
    List<ConceptSet> selectedConceptSets =
        conceptSetService.listConceptSets(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.CONCEPT_SET),
                Set.of(ResourceId.forConceptSet(study1.getId(), "123"))),
            0,
            10);
    assertTrue(selectedConceptSets.isEmpty());

    // Get invalid concept set.
    assertThrows(NotFoundException.class, () -> conceptSetService.getConceptSet("789", "123"));
    assertThrows(
        NotFoundException.class, () -> conceptSetService.getConceptSet(study1.getId(), "123"));

    // Specify invalid underlay.
    assertThrows(
        NotFoundException.class,
        () ->
            conceptSetService.createConceptSet(
                study1.getId(),
                ConceptSet.builder().underlay("invalid_underlay").entity(GENDER_EQ_WOMAN.getKey()),
                "abc@123.com"));
  }
}
