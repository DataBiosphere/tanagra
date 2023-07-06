package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_1;
import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_2;
import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.inmemory.InMemoryRowResult;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.artifact.*;
import java.sql.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
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
  private Cohort cohort1;
  private Cohort cohort2;
  private Review review1;
  private Review review2;
  private Review review3;
  private Review review4;

  @BeforeEach
  void createCohortsAndReviews() {
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

    // Create review1 for cohort1.
    ColumnHeaderSchema columnHeaderSchema =
        new ColumnHeaderSchema(List.of(new ColumnSchema("id", CellValue.SQLDataType.INT64)));
    QueryResult queryResult =
        new QueryResult(
            List.of(10L, 11L, 12L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    review1 =
        reviewService.createReviewHelper(
            study1.getId(), cohort1.getId(), Review.builder().size(11), userEmail, queryResult);
    assertNotNull(review1);
    LOGGER.info("Created review {} at {}", review1.getId(), review1.getCreated());

    // Create review2 and review3 for cohort2.
    queryResult =
        new QueryResult(
            List.of(20L, 21L, 22L, 24L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    review2 =
        reviewService.createReviewHelper(
            study1.getId(), cohort2.getId(), Review.builder().size(14), userEmail, queryResult);
    assertNotNull(review2);
    LOGGER.info("Created review {} at {}", review2.getId(), review2.getCreated());
    queryResult =
        new QueryResult(
            List.of(24L, 25L, 26L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    review3 =
        reviewService.createReviewHelper(
            study1.getId(), cohort2.getId(), Review.builder().size(3), userEmail, queryResult);
    assertNotNull(review3);
    LOGGER.info("Created review {} at {}", review3.getId(), review3.getCreated());
    queryResult =
        new QueryResult(
            List.of(22L, 23L, 24L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    review4 =
        reviewService.createReviewHelper(
            study1.getId(), cohort2.getId(), Review.builder().size(4), userEmail, queryResult);
    assertNotNull(review4);
    LOGGER.info("Created review {} at {}", review4.getId(), review4.getCreated());
  }

  @AfterEach
  void deleteCohortsAndReviews() {
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
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.ANNOTATION_KEY, ResourceId.forCohort(study1.getId(), cohort2.getId())),
            0,
            10);
    assertEquals(2, allAnnotationKeys.size());
    LOGGER.info(
        "Annotation keys found: {}, {}",
        allAnnotationKeys.get(0).getId(),
        allAnnotationKeys.get(1).getId());

    // List selected annotation key for cohort2.
    List<AnnotationKey> selectedAnnotationKeys =
        annotationService.listAnnotationKeys(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.ANNOTATION_KEY),
                Set.of(
                    ResourceId.forAnnotationKey(
                        study1.getId(), cohort2.getId(), annotationKey3.getId()))),
            0,
            10);
    assertEquals(1, selectedAnnotationKeys.size());
  }

  @Test
  void invalidKey() {
    // List all.
    List<AnnotationKey> allAnnotationKeys =
        annotationService.listAnnotationKeys(
            ResourceCollection.allResourcesAllPermissions(
                ResourceType.ANNOTATION_KEY, ResourceId.forCohort(study1.getId(), cohort1.getId())),
            0,
            10);
    assertTrue(allAnnotationKeys.isEmpty());

    // List selected.
    List<AnnotationKey> selectedAnnotationKeys =
        annotationService.listAnnotationKeys(
            ResourceCollection.resourcesSamePermissions(
                Permissions.allActions(ResourceType.ANNOTATION_KEY),
                Set.of(ResourceId.forAnnotationKey(study1.getId(), cohort1.getId(), "123"))),
            0,
            10);
    assertTrue(selectedAnnotationKeys.isEmpty());

    // Get invalid annotation key.
    assertThrows(
        NotFoundException.class, () -> annotationService.getAnnotationKey("789", "123", "456"));
    assertThrows(
        NotFoundException.class,
        () -> annotationService.getAnnotationKey(study1.getId(), cohort1.getId(), "123"));

    // Don't specify the display name.
    assertThrows(
        BadRequestException.class,
        () ->
            annotationService.createAnnotationKey(
                study1.getId(),
                cohort1.getId(),
                AnnotationKey.builder().dataType(Literal.DataType.INT64)));
  }

  @Test
  void keyWithEnumVals() {
    // Create.
    List<String> enumVals = List.of("STATUS", "NOTES");
    AnnotationKey createdAnnotationKey =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder()
                .dataType(Literal.DataType.STRING)
                .enumVals(enumVals)
                .displayName("key1"));
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

  @Test
  void createUpdateDeleteValues() {
    String instanceId = "11";

    // Create an integer annotation key and value.
    AnnotationKey annotationKeyInt =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.INT64).displayName("intkey"));
    assertNotNull(annotationKeyInt);
    LOGGER.info("Created annotation key {}", annotationKeyInt.getId());

    Literal intVal = new Literal(16L);
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKeyInt.getId(),
        review1.getId(),
        instanceId,
        List.of(intVal));
    LOGGER.info("Created annotation value");

    List<AnnotationValue> allVals =
        reviewService.listAnnotationValues(study1.getId(), cohort1.getId());
    AnnotationValue intAnnotationVal =
        allVals.stream()
            .filter(av -> av.getAnnotationKeyId().equals(annotationKeyInt.getId()))
            .findFirst()
            .get();
    assertEquals(intVal, intAnnotationVal.getLiteral());

    // Create a boolean annotation key and value.
    AnnotationKey annotationKeyBool =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.BOOLEAN).displayName("boolkey"));
    assertNotNull(annotationKeyBool);
    LOGGER.info("Created annotation key {}", annotationKeyBool.getId());

    Literal boolVal = new Literal(true);
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKeyBool.getId(),
        review1.getId(),
        instanceId,
        List.of(boolVal));
    LOGGER.info("Created annotation value");

    allVals = reviewService.listAnnotationValues(study1.getId(), cohort1.getId());
    AnnotationValue boolAnnotationVal =
        allVals.stream()
            .filter(av -> av.getAnnotationKeyId().equals(annotationKeyBool.getId()))
            .findFirst()
            .get();
    assertEquals(boolVal, boolAnnotationVal.getLiteral());

    // Create a string enum annotation key and value.
    AnnotationKey annotationKeyStrEnum =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder()
                .dataType(Literal.DataType.STRING)
                .enumVals(List.of("STATUS", "NOTES"))
                .displayName("enumkey"));
    assertNotNull(annotationKeyStrEnum);
    LOGGER.info("Created annotation key {}", annotationKeyStrEnum.getId());

    Literal enumVal = new Literal("STATUS");
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKeyStrEnum.getId(),
        review1.getId(),
        instanceId,
        List.of(enumVal));
    LOGGER.info("Created annotation value");

    allVals = reviewService.listAnnotationValues(study1.getId(), cohort1.getId());
    AnnotationValue enumAnnotationVal =
        allVals.stream()
            .filter(av -> av.getAnnotationKeyId().equals(annotationKeyStrEnum.getId()))
            .findFirst()
            .get();
    assertEquals(enumVal, enumAnnotationVal.getLiteral());

    // Create a date annotation key and value.
    AnnotationKey annotationKeyDate =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.DATE).displayName("datekey"));
    assertNotNull(annotationKeyDate);
    LOGGER.info("Created annotation key {}", annotationKeyDate.getId());

    Literal dateVal = Literal.forDate(new Date(System.currentTimeMillis()).toString());
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKeyDate.getId(),
        review1.getId(),
        instanceId,
        List.of(dateVal));
    LOGGER.info("Created annotation value");

    allVals = reviewService.listAnnotationValues(study1.getId(), cohort1.getId());
    AnnotationValue dateAnnotationVal =
        allVals.stream()
            .filter(av -> av.getAnnotationKeyId().equals(annotationKeyDate.getId()))
            .findFirst()
            .get();
    assertEquals(dateVal, dateAnnotationVal.getLiteral());

    // Update all 4 annotation values.
    Literal updatedIntVal = new Literal(15L);
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKeyInt.getId(),
        review1.getId(),
        instanceId,
        List.of(updatedIntVal));

    Literal updatedBoolVal = new Literal(false);
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKeyBool.getId(),
        review1.getId(),
        instanceId,
        List.of(updatedBoolVal));

    Literal updatedEnumVal = new Literal("NOTES");
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKeyStrEnum.getId(),
        review1.getId(),
        instanceId,
        List.of(updatedEnumVal));

    Literal updatedDateVal =
        Literal.forDate(new Date(System.currentTimeMillis() + 1000).toString());
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKeyDate.getId(),
        review1.getId(),
        instanceId,
        List.of(updatedDateVal));

    allVals = reviewService.listAnnotationValues(study1.getId(), cohort1.getId());
    assertEquals(4, allVals.size());

    intAnnotationVal =
        allVals.stream()
            .filter(av -> av.getAnnotationKeyId().equals(annotationKeyInt.getId()))
            .findFirst()
            .get();
    assertEquals(updatedIntVal, intAnnotationVal.getLiteral());

    boolAnnotationVal =
        allVals.stream()
            .filter(av -> av.getAnnotationKeyId().equals(annotationKeyBool.getId()))
            .findFirst()
            .get();
    assertEquals(updatedBoolVal, boolAnnotationVal.getLiteral());

    enumAnnotationVal =
        allVals.stream()
            .filter(av -> av.getAnnotationKeyId().equals(annotationKeyStrEnum.getId()))
            .findFirst()
            .get();
    assertEquals(updatedEnumVal, enumAnnotationVal.getLiteral());

    dateAnnotationVal =
        allVals.stream()
            .filter(av -> av.getAnnotationKeyId().equals(annotationKeyDate.getId()))
            .findFirst()
            .get();
    assertEquals(updatedDateVal, dateAnnotationVal.getLiteral());

    // Delete all 4 values.
    annotationService.deleteAnnotationValues(
        study1.getId(), cohort1.getId(), annotationKeyInt.getId(), review1.getId(), instanceId);
    annotationService.deleteAnnotationValues(
        study1.getId(), cohort1.getId(), annotationKeyBool.getId(), review1.getId(), instanceId);
    annotationService.deleteAnnotationValues(
        study1.getId(), cohort1.getId(), annotationKeyStrEnum.getId(), review1.getId(), instanceId);
    annotationService.deleteAnnotationValues(
        study1.getId(), cohort1.getId(), annotationKeyDate.getId(), review1.getId(), instanceId);

    allVals = reviewService.listAnnotationValues(study1.getId(), cohort1.getId());
    assertTrue(allVals.isEmpty());
  }

  @Test
  void listAllValuesAcrossReviews() {
    // Create one annotation key and two values for cohort1, review1.
    AnnotationKey annotationKeyInt1 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.INT64).displayName("key1"));
    assertNotNull(annotationKeyInt1);
    LOGGER.info("Created annotation key {}", annotationKeyInt1.getId());
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKeyInt1.getId(),
        review1.getId(),
        "12",
        List.of(new Literal(16L), new Literal(26L)));
    LOGGER.info("Created two annotation values");

    // Create one annotation key and value for cohort2, review2.
    AnnotationKey annotationKeyStr1 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort2.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.STRING).displayName("key2"));
    assertNotNull(annotationKeyStr1);
    LOGGER.info("Created annotation key {}", annotationKeyStr1.getId());
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort2.getId(),
        annotationKeyStr1.getId(),
        review2.getId(),
        "24",
        List.of(new Literal("val 1")));
    LOGGER.info("Created two annotation values");

    List<AnnotationValue> allVals2 =
        reviewService.listAnnotationValues(study1.getId(), cohort2.getId());
    assertEquals(1, allVals2.size());

    // Create one annotation key and value for cohort2, review3.
    AnnotationKey annotationKeyStr2 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort2.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.STRING).displayName("key3"));
    assertNotNull(annotationKeyStr2);
    LOGGER.info("Created annotation key {}", annotationKeyStr2.getId());
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort2.getId(),
        annotationKeyStr2.getId(),
        review3.getId(),
        "25",
        List.of(new Literal("val 2")));
    LOGGER.info("Created annotation value");

    // List all annotation values for cohort1 (review1 only).
    List<AnnotationValue> allVals =
        reviewService.listAnnotationValues(study1.getId(), cohort1.getId());
    assertEquals(2, allVals.size());

    // List all annotation values for cohort2 (review2 and review3 both).
    allVals = reviewService.listAnnotationValues(study1.getId(), cohort2.getId());
    assertEquals(2, allVals.size());

    // Create a new value for cohort2, review3 that has the same instance id as a value in cohort2,
    // review2.
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort2.getId(),
        annotationKeyStr1.getId(),
        review3.getId(),
        "24",
        List.of(new Literal("val 3")));
    LOGGER.info("Created annotation value");

    // Create a new value for cohort2, review4 that has the same instance id as a value in cohort2,
    // review2 and review3.
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort2.getId(),
        annotationKeyStr2.getId(),
        review4.getId(),
        "24",
        List.of(new Literal("val 4")));
    LOGGER.info("Created annotation value");

    // List all annotation values for cohort2, with review2 selected.
    allVals = reviewService.listAnnotationValues(study1.getId(), cohort2.getId(), review2.getId());
    assertEquals(4, allVals.size());

    // List all annotation values for cohort2, with review3 selected.
    allVals = reviewService.listAnnotationValues(study1.getId(), cohort2.getId(), review3.getId());
    assertEquals(3, allVals.size());

    // List all annotation values for cohort2, with review4 selected.
    allVals = reviewService.listAnnotationValues(study1.getId(), cohort2.getId(), review4.getId());
    assertEquals(3, allVals.size());

    // List all annotation values for cohort2, with no review selected.
    allVals = reviewService.listAnnotationValues(study1.getId(), cohort2.getId());
    assertEquals(3, allVals.size());
  }

  @Test
  void invalidValues() {
    // List all.
    List<AnnotationValue> allVals =
        reviewService.listAnnotationValues(study1.getId(), cohort1.getId());
    assertTrue(allVals.isEmpty());

    // Create an annotation key for cohort2.
    AnnotationKey annotationKeyStr =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort2.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.STRING).displayName("key1"));
    assertNotNull(annotationKeyStr);
    LOGGER.info("Created annotation key {}", annotationKeyStr.getId());

    // Use an invalid review + instance id combination.
    // TODO: Wrap this with a more user-friendly exception.
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            annotationService.updateAnnotationValues(
                study1.getId(),
                cohort2.getId(),
                annotationKeyStr.getId(),
                review1.getId(),
                "25",
                List.of(new Literal("val 1"), new Literal("val 2"))));

    // Use an invalid cohort + annotation key combination.
    assertThrows(
        NotFoundException.class,
        () ->
            annotationService.updateAnnotationValues(
                study1.getId(),
                cohort1.getId(),
                annotationKeyStr.getId(),
                review1.getId(),
                "10",
                List.of(new Literal("val 3"))));

    // Use an invalid cohort + review combination.
    // TODO: Wrap this with a more user-friendly exception.
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            annotationService.updateAnnotationValues(
                study1.getId(),
                cohort2.getId(),
                annotationKeyStr.getId(),
                review1.getId(),
                "10",
                List.of(new Literal("val 4"))));
  }

  @Test
  void tsv() {
    // Create one annotation key and value for cohort2, review2.
    AnnotationKey annotationKeyStr1 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort2.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.STRING).displayName("key1"));
    assertNotNull(annotationKeyStr1);
    LOGGER.info("Created annotation key {}", annotationKeyStr1.getId());
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort2.getId(),
        annotationKeyStr1.getId(),
        review2.getId(),
        "24",
        List.of(new Literal("val 1")));
    LOGGER.info("Created annotation value");

    // Create one annotation key and value for cohort2, review3.
    AnnotationKey annotationKeyStr2 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort2.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.STRING).displayName("key2"));
    assertNotNull(annotationKeyStr2);
    LOGGER.info("Created annotation key {}", annotationKeyStr2.getId());
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort2.getId(),
        annotationKeyStr2.getId(),
        review3.getId(),
        "25",
        List.of(new Literal("val 2")));
    LOGGER.info("Created annotation value");

    // Generate a TSV string with the annotation values data.
    String tsv = reviewService.buildTsvStringForAnnotationValues(study1.getId(), cohort2.getId());
    assertEquals("person_id\tkey1\tkey2\n24\tval 1\t\n25\t\tval 2\n", tsv);
  }
}
