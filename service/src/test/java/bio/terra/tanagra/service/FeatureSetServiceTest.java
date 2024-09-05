package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.CONDITION_EQ_TYPE_2_DIABETES;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.GENDER_EQ_WOMAN;
import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.Criteria.PROCEDURE_EQ_AMPUTATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.exception.*;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.FeatureSetService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.util.CollectionUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
public class FeatureSetServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureSetServiceTest.class);
  private static final String UNDERLAY_NAME = "cmssynpuf";
  private static final String USER_EMAIL_1 = "abc@123.com";
  private static final String USER_EMAIL_2 = "def@123.com";
  private static final List<String> PERSON_ATTRIBUTES = List.of("gender", "age");

  @Autowired private StudyService studyService;
  @Autowired private FeatureSetService featureSetService;

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
      studyService.deleteStudy(study2.getId(), USER_EMAIL_2);
      LOGGER.info("Deleted study2 {}", study2.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study2", ex);
    }
  }

  @Test
  void createUpdateDelete() throws InterruptedException {
    // Create.
    String displayName = "feature set 1";
    String description = "first feature set";
    FeatureSet createdFeatureSet =
        featureSetService.createFeatureSet(
            study1.getId(),
            FeatureSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName(displayName)
                .description(description)
                .criteria(List.of(GENDER_EQ_WOMAN.getValue()))
                .excludeOutputAttributesPerEntity(
                    Map.of(GENDER_EQ_WOMAN.getKey(), PERSON_ATTRIBUTES)),
            USER_EMAIL_1);
    assertNotNull(createdFeatureSet);
    LOGGER.info(
        "Created feature set {} at {}", createdFeatureSet.getId(), createdFeatureSet.getCreated());
    assertEquals(UNDERLAY_NAME, createdFeatureSet.getUnderlay());
    assertEquals(displayName, createdFeatureSet.getDisplayName());
    assertEquals(description, createdFeatureSet.getDescription());
    assertEquals(USER_EMAIL_1, createdFeatureSet.getCreatedBy());
    assertEquals(USER_EMAIL_1, createdFeatureSet.getLastModifiedBy());
    assertEquals(createdFeatureSet.getCreated(), createdFeatureSet.getLastModified());
    assertEquals(1, createdFeatureSet.getCriteria().size());
    assertTrue(createdFeatureSet.getCriteria().contains(GENDER_EQ_WOMAN.getValue()));
    assertEquals(1, createdFeatureSet.getExcludeOutputAttributesPerEntity().keySet().size());
    assertEquals(
        PERSON_ATTRIBUTES.stream().sorted().toList(),
        createdFeatureSet
            .getExcludeOutputAttributesPerEntity()
            .get(GENDER_EQ_WOMAN.getKey())
            .stream()
            .sorted()
            .toList());

    // Update.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    String displayName2 = "feature set 1 updated";
    String description2 = "first feature set updated";
    String outputEntity = "conditionOccurrence";
    List<String> outputAttributes = List.of("condition", "person_id");
    FeatureSet updatedFeatureSet =
        featureSetService.updateFeatureSet(
            study1.getId(),
            createdFeatureSet.getId(),
            USER_EMAIL_2,
            displayName2,
            description2,
            List.of(CONDITION_EQ_TYPE_2_DIABETES.getValue()),
            Map.of(outputEntity, outputAttributes));
    assertNotNull(updatedFeatureSet);
    LOGGER.info(
        "Updated feature set {} at {}",
        updatedFeatureSet.getId(),
        updatedFeatureSet.getLastModified());
    assertEquals(createdFeatureSet.getUnderlay(), updatedFeatureSet.getUnderlay());
    assertEquals(
        1,
        featureSetService
            .listFeatureSets(
                ResourceCollection.allResourcesAllPermissions(
                    ResourceType.FEATURE_SET, ResourceId.forStudy(study1.getId())),
                0,
                10)
            .size());
    assertEquals(displayName2, updatedFeatureSet.getDisplayName());
    assertEquals(description2, updatedFeatureSet.getDescription());
    assertEquals(USER_EMAIL_1, updatedFeatureSet.getCreatedBy());
    assertEquals(USER_EMAIL_2, updatedFeatureSet.getLastModifiedBy());
    assertTrue(updatedFeatureSet.getLastModified().isAfter(updatedFeatureSet.getCreated()));
    assertEquals(1, updatedFeatureSet.getCriteria().size());
    assertTrue(updatedFeatureSet.getCriteria().contains(CONDITION_EQ_TYPE_2_DIABETES.getValue()));
    assertEquals(1, updatedFeatureSet.getExcludeOutputAttributesPerEntity().keySet().size());
    assertEquals(
        outputAttributes.stream().sorted().toList(),
        updatedFeatureSet.getExcludeOutputAttributesPerEntity().get(outputEntity).stream()
            .sorted()
            .toList());

    // Delete.
    featureSetService.deleteFeatureSet(study1.getId(), createdFeatureSet.getId());
    List<FeatureSet> featureSets =
        featureSetService.listFeatureSets(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.FEATURE_SET, ResourceId.forStudy(study1.getId())),
            0,
            10);
    assertTrue(featureSets.isEmpty());
    FeatureSet featureSet =
        featureSetService.getFeatureSet(study1.getId(), createdFeatureSet.getId());
    assertTrue(featureSet.isDeleted());
  }

  @Test
  void listAllOrSelected() {
    // Create one feature set in study1.
    FeatureSet featureSet1 =
        featureSetService.createFeatureSet(
            study1.getId(),
            FeatureSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("feature set 1")
                .description("first feature set")
                .criteria(List.of(DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE.getRight()))
                .excludeOutputAttributesPerEntity(
                    Map.of(DEMOGRAPHICS_PREPACKAGED_DATA_FEATURE.getKey(), PERSON_ATTRIBUTES)),
            USER_EMAIL_1);
    assertNotNull(featureSet1);
    LOGGER.info("Created feature set {} at {}", featureSet1.getId(), featureSet1.getCreated());

    // Create two feature sets in study2.
    FeatureSet featureSet2 =
        featureSetService.createFeatureSet(
            study2.getId(),
            FeatureSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("feature set 2")
                .description("second feature set")
                .criteria(List.of(PROCEDURE_EQ_AMPUTATION.getValue()))
                .excludeOutputAttributesPerEntity(
                    Map.of("procedureOccurrence", List.of("procedure", "person_id"))),
            USER_EMAIL_1);
    assertNotNull(featureSet2);
    LOGGER.info("Created feature set {} at {}", featureSet2.getId(), featureSet2.getCreated());
    FeatureSet featureSet3 =
        featureSetService.createFeatureSet(
            study2.getId(),
            FeatureSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("feature set 3")
                .description("third feature set")
                .criteria(List.of(GENDER_EQ_WOMAN.getValue()))
                .excludeOutputAttributesPerEntity(
                    Map.of(GENDER_EQ_WOMAN.getKey(), PERSON_ATTRIBUTES)),
            USER_EMAIL_1);
    assertNotNull(featureSet3);
    LOGGER.info("Created feature set {} at {}", featureSet3.getId(), featureSet3.getCreated());

    // List all cohorts in study2. Those from study1 are not displayed
    List<FeatureSet> allFeatureSets =
        featureSetService.listFeatureSets(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.FEATURE_SET, ResourceId.forStudy(study2.getId())),
            0,
            10);
    assertEquals(2, allFeatureSets.size());
    LOGGER.info(
        "feature sets found: {}, {}", allFeatureSets.get(0).getId(), allFeatureSets.get(1).getId());
    List<FeatureSet> allFeatureSetsSortedByDisplayNameAsc =
        allFeatureSets.stream().sorted(Comparator.comparing(FeatureSet::getDisplayName)).toList();
    assertEquals(allFeatureSets, allFeatureSetsSortedByDisplayNameAsc);

    // List selected feature set in study2.
    List<FeatureSet> selectedFeatureSets =
        featureSetService.listFeatureSets(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.FEATURE_SET),
                Set.of(ResourceId.forFeatureSet(study2.getId(), featureSet3.getId()))),
            0,
            10);
    assertEquals(1, selectedFeatureSets.size());
  }

  @Test
  void cloneFeatureSet() {
    // Create first feature set without criteria or attributes
    FeatureSet featureSet1 =
        featureSetService.createFeatureSet(
            study1.getId(),
            FeatureSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("feature set 1")
                .description("first feature set"),
            USER_EMAIL_1);
    assertNotNull(featureSet1);
    LOGGER.info("Created feature set {} at {}", featureSet1.getId(), featureSet1.getCreated());

    // Clone the feature set without new displayName and verify
    String newDescription = "cloned feature set description";
    FeatureSet clonedFeatureSet1 =
        featureSetService.cloneFeatureSet(
            study1.getId(), featureSet1.getId(), USER_EMAIL_2, null, newDescription);
    assertEquals(
        2,
        featureSetService
            .listFeatureSets(
                ResourceCollection.allResourcesAllPermissions(
                    ResourceType.FEATURE_SET, ResourceId.forStudy(study1.getId())),
                0,
                10)
            .size());
    assertEquals(featureSet1.getUnderlay(), clonedFeatureSet1.getUnderlay());
    assertEquals(featureSet1.getDisplayName(), clonedFeatureSet1.getDisplayName());
    assertEquals(newDescription, clonedFeatureSet1.getDescription());
    assertEquals(USER_EMAIL_2, clonedFeatureSet1.getCreatedBy());
    assertEquals(USER_EMAIL_2, clonedFeatureSet1.getLastModifiedBy());
    assertEquals(clonedFeatureSet1.getCreated(), clonedFeatureSet1.getLastModified());
    assertTrue(CollectionUtils.isEmpty(clonedFeatureSet1.getCriteria()));
    assertTrue(CollectionUtils.isEmpty(clonedFeatureSet1.getExcludeOutputAttributesPerEntity()));

    // Create second feature set with criteria and attributes
    List<Criteria> criteria2 = List.of(GENDER_EQ_WOMAN.getValue());
    Map<String, List<String>> attributes2 = Map.of(GENDER_EQ_WOMAN.getKey(), PERSON_ATTRIBUTES);
    FeatureSet featureSet2 =
        featureSetService.createFeatureSet(
            study1.getId(),
            FeatureSet.builder()
                .underlay(UNDERLAY_NAME)
                .displayName("feature set 2")
                .description("second feature set")
                .criteria(criteria2)
                .excludeOutputAttributesPerEntity(attributes2),
            USER_EMAIL_2);
    assertNotNull(featureSet2);
    LOGGER.info("Created feature set {} at {}", featureSet2.getId(), featureSet2.getCreated());

    // Clone the feature set without new description and verify
    String newDisplayName = "cloned feature set displayName";
    FeatureSet clonedFeatureSet2 =
        featureSetService.cloneFeatureSet(
            study1.getId(), featureSet2.getId(), USER_EMAIL_2, newDisplayName, null);
    assertEquals(
        4,
        featureSetService
            .listFeatureSets(
                ResourceCollection.allResourcesAllPermissions(
                    ResourceType.FEATURE_SET, ResourceId.forStudy(study1.getId())),
                0,
                10)
            .size());
    assertEquals(newDisplayName, clonedFeatureSet2.getDisplayName());
    assertEquals(featureSet2.getDescription(), clonedFeatureSet2.getDescription());

    List<Criteria> clonedCriteria2 = clonedFeatureSet2.getCriteria();
    assertEquals(criteria2.size(), clonedCriteria2.size());
    for (int i = 0; i < criteria2.size(); ++i) {
      assertEquals(criteria2.get(i), clonedCriteria2.get(i));
    }

    Map<String, List<String>> clonedAttributes2 =
        clonedFeatureSet2.getExcludeOutputAttributesPerEntity();
    assertEquals(attributes2.size(), clonedAttributes2.size());
    attributes2.forEach(
        (key, value) ->
            assertEquals(
                value.stream().sorted().toList(), clonedAttributes2.get(key).stream().toList()));

    // Update original criteria
    String updatedCriteriaDisplay = "new criteria display name";
    Criteria updatedCriteria =
        Criteria.builder()
            .id(criteria2.get(0).getId())
            .displayName(updatedCriteriaDisplay)
            .pluginName(criteria2.get(0).getPluginName())
            .pluginVersion(criteria2.get(0).getPluginVersion())
            .predefinedId(criteria2.get(0).getPredefinedId())
            .selectorOrModifierName(criteria2.get(0).getSelectorOrModifierName())
            .selectionData(criteria2.get(0).getSelectionData())
            .uiConfig(criteria2.get(0).getUiConfig())
            .tags(criteria2.get(0).getTags())
            .build();
    FeatureSet updatedFeatureSet =
        featureSetService.updateFeatureSet(
            study1.getId(),
            featureSet2.getId(),
            USER_EMAIL_1,
            null,
            null,
            List.of(updatedCriteria),
            null);
    assertEquals(updatedCriteriaDisplay, updatedFeatureSet.getCriteria().get(0).getDisplayName());

    // Verily that clone is not changed
    FeatureSet getClonedFeatureSet2 =
        featureSetService.getFeatureSet(study1.getId(), clonedFeatureSet2.getId());
    assertEquals(
        clonedFeatureSet2.getCriteria().get(0).getDisplayName(),
        getClonedFeatureSet2.getCriteria().get(0).getDisplayName());
  }

  @Test
  void invalid() {
    // List all.
    List<FeatureSet> allFeatureSets =
        featureSetService.listFeatureSets(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.FEATURE_SET, ResourceId.forStudy(study1.getId())),
            0,
            10);
    assertTrue(allFeatureSets.isEmpty());

    // List selected.
    List<FeatureSet> selectedFeatureSets =
        featureSetService.listFeatureSets(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.FEATURE_SET),
                Set.of(ResourceId.forFeatureSet(study1.getId(), "123"))),
            0,
            10);
    assertTrue(selectedFeatureSets.isEmpty());

    // Get invalid feature set.
    assertThrows(NotFoundException.class, () -> featureSetService.getFeatureSet("789", "123"));
    assertThrows(
        NotFoundException.class, () -> featureSetService.getFeatureSet(study1.getId(), "123"));

    // Specify invalid underlay.
    assertThrows(
        NotFoundException.class,
        () ->
            featureSetService.createFeatureSet(
                study1.getId(), FeatureSet.builder().underlay("invalid_underlay"), USER_EMAIL_1));

    // Display name length exceeds maximum.
    assertThrows(
        BadRequestException.class,
        () ->
            featureSetService.createFeatureSet(
                study1.getId(),
                FeatureSet.builder()
                    .underlay(UNDERLAY_NAME)
                    .displayName("123456789012345678901234567890123456789012345678901"),
                USER_EMAIL_1));

    // TODO: Put this validation test back once the UI config overhaul is complete.
    //    // Specify invalid attribute.
    //    assertThrows(
    //        NotFoundException.class,
    //        () ->
    //            featureSetService.createFeatureSet(
    //                study1.getId(),
    //                FeatureSet.builder()
    //                    .underlay("invalid_underlay")
    //                    .excludeOutputAttributesPerEntity(
    //                        Map.of(GENDER_EQ_WOMAN.getKey(), List.of("invalid_attribute"))),
    //                USER_EMAIL_1));
  }
}
