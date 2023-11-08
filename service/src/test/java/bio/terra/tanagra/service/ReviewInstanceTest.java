package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_1;
import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.api2.filter.AttributeFilter;
import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.query.inmemory.InMemoryRowResult;
import bio.terra.tanagra.service.artifact.AnnotationService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.AnnotationValue;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.query.ReviewInstance;
import bio.terra.tanagra.service.query.ReviewQueryOrderBy;
import bio.terra.tanagra.service.query.ReviewQueryRequest;
import bio.terra.tanagra.service.query.UnderlayService;
import bio.terra.tanagra.service.query.filter.AnnotationFilter;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Attribute;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
@Tag("requires-cloud-access")
public class ReviewInstanceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewInstanceTest.class);
  private static final String UNDERLAY_NAME = "cms_synpuf";

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
  @Autowired private AnnotationService annotationService;
  @Autowired private ReviewService reviewService;
  @Autowired private UnderlayService underlayService;

  private Study study1;
  private Cohort cohort1;
  private Review review1;
  private Review review2;
  private Review review3;
  private Review review4;
  private AnnotationKey annotationKey1;
  private AnnotationKey annotationKey2;

  @BeforeEach
  void createReviewsAndAnnotations() {
    String userEmail = "abc@123.com";

    // Create study1.
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
    LOGGER.info("Created cohort1 {} at {}", cohort1.getId(), cohort1.getCreated());

    // Create review1, review2, review3, review4 for cohort1.
    // r1: 2014950, 1858841, 2180409
    // r2: 1858841, 2180409, 1131436, 1838382
    // r3: 1858841, 2180409, 1838382
    // r4: 1858841, 1838382, 799353, 2104705
    ColumnHeaderSchema columnHeaderSchema =
        new ColumnHeaderSchema(List.of(new ColumnSchema("id", CellValue.SQLDataType.INT64)));
    QueryResult queryResult =
        new QueryResult(
            List.of(2_014_950L, 1_858_841L, 2_180_409L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    review1 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort1.getId(),
            Review.builder().size(11),
            userEmail,
            queryResult,
            1_500_000L);
    assertNotNull(review1);
    LOGGER.info("Created review1 {} at {}", review1.getId(), review1.getCreated());

    queryResult =
        new QueryResult(
            List.of(1_858_841L, 2_180_409L, 1_131_436L, 1_838_382L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    review2 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort1.getId(),
            Review.builder().size(14),
            userEmail,
            queryResult,
            1_500_000L);
    assertNotNull(review2);
    LOGGER.info("Created review2 {} at {}", review2.getId(), review2.getCreated());

    queryResult =
        new QueryResult(
            List.of(1_858_841L, 2_180_409L, 1_838_382L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    review3 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort1.getId(),
            Review.builder().size(3),
            userEmail,
            queryResult,
            1_500_000L);
    assertNotNull(review3);
    LOGGER.info("Created review3 {} at {}", review3.getId(), review3.getCreated());

    queryResult =
        new QueryResult(
            List.of(1_858_841L, 1_838_382L, 799_353L, 2_104_705L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    review4 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort1.getId(),
            Review.builder().size(4),
            userEmail,
            queryResult,
            1_500_000L);
    assertNotNull(review4);
    LOGGER.info("Created review4 {} at {}", review4.getId(), review4.getCreated());

    // Create annotationKey1 for cohort1.
    annotationKey1 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.INT64).displayName("key1"));
    assertNotNull(annotationKey1);
    LOGGER.info("Created annotationKey1 {}", annotationKey1.getId());

    // Create annotationKey2 for cohort1.
    annotationKey2 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder().dataType(Literal.DataType.STRING).displayName("key2"));
    assertNotNull(annotationKey2);
    LOGGER.info("Created annotationKey2 {}", annotationKey2.getId());

    // Create annotation values.
    // 2014950: (r1, k1)
    // 1858841: (r1, k1), (r2, k1), (r3, k1), (r4, k1), (r1, k2), (r2, k2), (r3, k2)
    // 2180409: (r2, k1), (r3, k1), (r1, k2), (r3, k2)
    // 1131436: (r2, k1)
    // 1838382: (r2, k2), (r3, k2), (r4, k2)
    // 799353:
    // 2104705: (r4, k1), (r4, k2)
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review1.getId(),
        "2014950",
        List.of(new Literal(111L)));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review1.getId(),
        "1858841",
        List.of(new Literal(112L)));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review2.getId(),
        "1858841",
        List.of(new Literal(113L)));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review3.getId(),
        "1858841",
        List.of(new Literal(114L)));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review4.getId(),
        "1858841",
        List.of(new Literal(115L)));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey2.getId(),
        review1.getId(),
        "1858841",
        List.of(new Literal("str116")));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey2.getId(),
        review2.getId(),
        "1858841",
        List.of(new Literal("str117")));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey2.getId(),
        review3.getId(),
        "1858841",
        List.of(new Literal("str118")));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review2.getId(),
        "2180409",
        List.of(new Literal(119L)));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review3.getId(),
        "2180409",
        List.of(new Literal(120L)));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey2.getId(),
        review1.getId(),
        "2180409",
        List.of(new Literal("str121")));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey2.getId(),
        review3.getId(),
        "2180409",
        List.of(new Literal("str122")));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review2.getId(),
        "1131436",
        List.of(new Literal(123L)));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey2.getId(),
        review2.getId(),
        "1838382",
        List.of(new Literal("str124")));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey2.getId(),
        review3.getId(),
        "1838382",
        List.of(new Literal("str125")));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey2.getId(),
        review4.getId(),
        "1838382",
        List.of(new Literal("str126")));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey1.getId(),
        review4.getId(),
        "2104705",
        List.of(new Literal(115L)));
    annotationService.updateAnnotationValues(
        study1.getId(),
        cohort1.getId(),
        annotationKey2.getId(),
        review4.getId(),
        "2104705",
        List.of(new Literal("str128")));
  }

  @AfterEach
  void deleteReviewsAndAnnotations() {
    try {
      studyService.deleteStudy(study1.getId(), "abc@123.com");
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }
  }

  @Test
  void entityAttributes() {
    Entity primaryEntity = underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();
    Attribute idAttr = primaryEntity.getIdAttribute();

    // List instances for review2. Request only the gender attribute.
    List<ReviewInstance> reviewInstances2 =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review2.getId(),
                ReviewQueryRequest.builder()
                    .attributes(List.of(primaryEntity.getAttribute("gender")))
                    .build())
            .getReviewInstances();
    checkEntityInstances(
        List.of(1_858_841L, 2_180_409L, 1_131_436L, 1_838_382L), idAttr, reviewInstances2);

    // Check that the id attribute was fetched automatically to each review instance.
    reviewInstances2.stream().forEach(ri -> assertNotNull(ri.getAttributeValues().get(idAttr)));

    // Check that the gender attribute values are correct.
    Attribute genderAttr = primaryEntity.getAttribute("gender");
    checkAttributeValue(genderAttr, 8_532L, 1_858_841L, idAttr, reviewInstances2);
    checkAttributeValue(genderAttr, 8_532L, 2_180_409L, idAttr, reviewInstances2);
    checkAttributeValue(genderAttr, 8_532L, 1_131_436L, idAttr, reviewInstances2);
    checkAttributeValue(genderAttr, 8_507L, 1_838_382L, idAttr, reviewInstances2);
  }

  @Test
  void annotationValues() {
    // List instances for reviews 1-4. For each, check that:
    //   - The correct entity instances (ids) are included.
    //   - The correct annotation values are included.
    //   - The isMostRecent and isPartOfSelectedReview flags are set correctly.
    Attribute primaryEntityIdAttribute =
        underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity().getIdAttribute();

    // List instances for review1.
    List<ReviewInstance> reviewInstances1 =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review1.getId(),
                ReviewQueryRequest.builder().build())
            .getReviewInstances();
    checkEntityInstances(
        List.of(2_014_950L, 1_858_841L, 2_180_409L), primaryEntityIdAttribute, reviewInstances1);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(111L))
                .cohortRevisionVersion(0)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("2014950")
                .isMostRecent(true)
                .isPartOfSelectedReview(true)
                .build()),
        2_014_950L,
        primaryEntityIdAttribute,
        reviewInstances1);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(112L))
                .cohortRevisionVersion(0)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("1858841")
                .isMostRecent(false)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal(115L))
                .cohortRevisionVersion(3)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("1858841")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str116"))
                .cohortRevisionVersion(0)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1858841")
                .isMostRecent(false)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str118"))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1858841")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build()),
        1_858_841L,
        primaryEntityIdAttribute,
        reviewInstances1);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(120L))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("2180409")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str121"))
                .cohortRevisionVersion(0)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("2180409")
                .isMostRecent(false)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str122"))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("2180409")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build()),
        2_180_409L,
        primaryEntityIdAttribute,
        reviewInstances1);

    // List instances for review2.
    List<ReviewInstance> reviewInstances2 =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review2.getId(),
                ReviewQueryRequest.builder().build())
            .getReviewInstances();
    checkEntityInstances(
        List.of(1_858_841L, 2_180_409L, 1_131_436L, 1_838_382L),
        primaryEntityIdAttribute,
        reviewInstances2);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(113L))
                .cohortRevisionVersion(1)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("1858841")
                .isMostRecent(false)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal(115L))
                .cohortRevisionVersion(3)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("1858841")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str117"))
                .cohortRevisionVersion(1)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1858841")
                .isMostRecent(false)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str118"))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1858841")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build()),
        1_858_841L,
        primaryEntityIdAttribute,
        reviewInstances2);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(119L))
                .cohortRevisionVersion(1)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("2180409")
                .isMostRecent(false)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal(120L))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("2180409")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str122"))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("2180409")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build()),
        2_180_409L,
        primaryEntityIdAttribute,
        reviewInstances2);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(123L))
                .cohortRevisionVersion(1)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("1131436")
                .isMostRecent(true)
                .isPartOfSelectedReview(true)
                .build()),
        1_131_436L,
        primaryEntityIdAttribute,
        reviewInstances2);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal("str124"))
                .cohortRevisionVersion(1)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1838382")
                .isMostRecent(false)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str126"))
                .cohortRevisionVersion(3)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1838382")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build()),
        1_838_382L,
        primaryEntityIdAttribute,
        reviewInstances2);

    // List instances for review3.
    List<ReviewInstance> reviewInstances3 =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review3.getId(),
                ReviewQueryRequest.builder().build())
            .getReviewInstances();
    checkEntityInstances(
        List.of(1_858_841L, 2_180_409L, 1_838_382L), primaryEntityIdAttribute, reviewInstances3);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(114L))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("1858841")
                .isMostRecent(false)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal(115L))
                .cohortRevisionVersion(3)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("1858841")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str118"))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1858841")
                .isMostRecent(true)
                .isPartOfSelectedReview(true)
                .build()),
        1_858_841L,
        primaryEntityIdAttribute,
        reviewInstances3);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(120L))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("2180409")
                .isMostRecent(true)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str122"))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("2180409")
                .isMostRecent(true)
                .isPartOfSelectedReview(true)
                .build()),
        2_180_409L,
        primaryEntityIdAttribute,
        reviewInstances3);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal("str125"))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1838382")
                .isMostRecent(false)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str126"))
                .cohortRevisionVersion(3)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1838382")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build()),
        1_838_382L,
        primaryEntityIdAttribute,
        reviewInstances3);

    // List instances for review4.
    List<ReviewInstance> reviewInstances4 =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review4.getId(),
                ReviewQueryRequest.builder().build())
            .getReviewInstances();
    checkEntityInstances(
        List.of(1_858_841L, 1_838_382L, 799_353L, 2_104_705L),
        primaryEntityIdAttribute,
        reviewInstances4);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(115L))
                .cohortRevisionVersion(3)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("1858841")
                .isMostRecent(true)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str118"))
                .cohortRevisionVersion(2)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1858841")
                .isMostRecent(true)
                .isPartOfSelectedReview(false)
                .build()),
        1_858_841L,
        primaryEntityIdAttribute,
        reviewInstances4);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal("str126"))
                .cohortRevisionVersion(3)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("1838382")
                .isMostRecent(true)
                .isPartOfSelectedReview(true)
                .build()),
        1_838_382L,
        primaryEntityIdAttribute,
        reviewInstances4);
    checkAnnotationValues(
        Collections.emptyList(), 799_353L, primaryEntityIdAttribute, reviewInstances4);
    checkAnnotationValues(
        List.of(
            AnnotationValue.builder()
                .literal(new Literal(115L))
                .cohortRevisionVersion(3)
                .annotationKeyId(annotationKey1.getId())
                .instanceId("2104705")
                .isMostRecent(true)
                .isPartOfSelectedReview(true)
                .build(),
            AnnotationValue.builder()
                .literal(new Literal("str128"))
                .cohortRevisionVersion(3)
                .annotationKeyId(annotationKey2.getId())
                .instanceId("2104705")
                .isMostRecent(true)
                .isPartOfSelectedReview(true)
                .build()),
        2_104_705L,
        primaryEntityIdAttribute,
        reviewInstances4);
  }

  @Test
  void orderBys() {
    Attribute primaryEntityIdAttribute =
        underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity().getIdAttribute();

    // Default order.
    List<ReviewInstance> reviewInstancesDefault =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review4.getId(),
                ReviewQueryRequest.builder().build())
            .getReviewInstances();
    reviewInstancesDefault.stream().forEach(rid -> LOGGER.info("si {}", rid.getStableIndex()));

    // Check the rows are returned in ascending stable index order.
    List<ReviewInstance> reviewInstancesOrderedByStableIndexAsc =
        reviewInstancesDefault.stream()
            .sorted(Comparator.comparing(ReviewInstance::getStableIndex))
            .collect(Collectors.toList());
    assertEquals(reviewInstancesOrderedByStableIndexAsc, reviewInstancesDefault);

    // Save the map of instance id -> stable index, so we can check that the index is in fact stable
    // across the following list calls with various order bys.
    Map<Long, Integer> reviewInstanceStableIndexMap =
        reviewInstancesDefault.stream()
            .collect(
                Collectors.toMap(
                    rid ->
                        rid.getAttributeValues()
                            .get(primaryEntityIdAttribute)
                            .getValue()
                            .getInt64Val(),
                    ReviewInstance::getStableIndex));

    // Order by an entity attribute.
    List<ReviewInstance> reviewInstancesByAttr =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review4.getId(),
                ReviewQueryRequest.builder()
                    .orderBys(
                        List.of(
                            new ReviewQueryOrderBy(
                                primaryEntityIdAttribute, OrderByDirection.DESCENDING)))
                    .build())
            .getReviewInstances();
    assertEquals(
        List.of(2_104_705L, 1_858_841L, 1_838_382L, 799_353L),
        reviewInstancesByAttr.stream()
            .map(
                ri ->
                    ri.getAttributeValues().get(primaryEntityIdAttribute).getValue().getInt64Val())
            .collect(Collectors.toList()));
    assertEquals(
        reviewInstanceStableIndexMap,
        reviewInstancesByAttr.stream()
            .collect(
                Collectors.toMap(
                    ri ->
                        ri.getAttributeValues()
                            .get(primaryEntityIdAttribute)
                            .getValue()
                            .getInt64Val(),
                    ReviewInstance::getStableIndex)));

    // Order by an annotation key.
    List<ReviewInstance> reviewInstancesByAnn =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review4.getId(),
                ReviewQueryRequest.builder()
                    .orderBys(
                        List.of(
                            new ReviewQueryOrderBy(annotationKey2, OrderByDirection.DESCENDING)))
                    .build())
            .getReviewInstances();
    assertEquals(
        List.of(2_104_705L, 1_838_382L, 1_858_841L, 799_353L),
        reviewInstancesByAnn.stream()
            .map(
                ri ->
                    ri.getAttributeValues().get(primaryEntityIdAttribute).getValue().getInt64Val())
            .collect(Collectors.toList()));
    assertEquals(
        reviewInstanceStableIndexMap,
        reviewInstancesByAnn.stream()
            .collect(
                Collectors.toMap(
                    ri ->
                        ri.getAttributeValues()
                            .get(primaryEntityIdAttribute)
                            .getValue()
                            .getInt64Val(),
                    ReviewInstance::getStableIndex)));

    // Order by both.
    List<ReviewInstance> reviewInstancesByBoth =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review4.getId(),
                ReviewQueryRequest.builder()
                    .orderBys(
                        List.of(
                            new ReviewQueryOrderBy(annotationKey1, OrderByDirection.ASCENDING),
                            new ReviewQueryOrderBy(
                                primaryEntityIdAttribute, OrderByDirection.ASCENDING)))
                    .build())
            .getReviewInstances();
    assertEquals(
        List.of(799_353L, 1_838_382L, 1_858_841L, 2_104_705L),
        reviewInstancesByBoth.stream()
            .map(
                ri ->
                    ri.getAttributeValues().get(primaryEntityIdAttribute).getValue().getInt64Val())
            .collect(Collectors.toList()));
    assertEquals(
        reviewInstanceStableIndexMap,
        reviewInstancesByBoth.stream()
            .collect(
                Collectors.toMap(
                    ri ->
                        ri.getAttributeValues()
                            .get(primaryEntityIdAttribute)
                            .getValue()
                            .getInt64Val(),
                    ReviewInstance::getStableIndex)));
  }

  @Test
  void filters() {
    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    Entity primaryEntity = underlay.getPrimaryEntity();

    // Filter by an entity attribute.
    List<ReviewInstance> reviewInstancesByAttr =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review4.getId(),
                ReviewQueryRequest.builder()
                    .entityFilter(
                        new AttributeFilter(
                            underlay,
                            primaryEntity,
                            primaryEntity.getAttribute("gender"),
                            BinaryFilterVariable.BinaryOperator.EQUALS,
                            new Literal(8_532L)))
                    .build())
            .getReviewInstances();
    assertEquals(
        List.of(1_858_841L),
        reviewInstancesByAttr.stream()
            .map(
                ri ->
                    ri.getAttributeValues()
                        .get(primaryEntity.getIdAttribute())
                        .getValue()
                        .getInt64Val())
            .collect(Collectors.toList()));

    // Filter by an annotation key.
    List<ReviewInstance> reviewInstancesByAnn =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review4.getId(),
                ReviewQueryRequest.builder()
                    .annotationFilter(
                        new AnnotationFilter(
                            annotationKey2,
                            BinaryFilterVariable.BinaryOperator.EQUALS,
                            new Literal("str128")))
                    .build())
            .getReviewInstances();
    assertEquals(
        List.of(2_104_705L),
        reviewInstancesByAnn.stream()
            .map(
                ri ->
                    ri.getAttributeValues()
                        .get(primaryEntity.getIdAttribute())
                        .getValue()
                        .getInt64Val())
            .collect(Collectors.toList()));

    // Filter by both.
    List<ReviewInstance> reviewInstancesByBoth =
        reviewService
            .listReviewInstances(
                study1.getId(),
                cohort1.getId(),
                review4.getId(),
                ReviewQueryRequest.builder()
                    .entityFilter(
                        new AttributeFilter(
                            underlay,
                            primaryEntity,
                            primaryEntity.getAttribute("gender"),
                            BinaryFilterVariable.BinaryOperator.EQUALS,
                            new Literal(8_507L)))
                    .annotationFilter(
                        new AnnotationFilter(
                            annotationKey1,
                            BinaryFilterVariable.BinaryOperator.EQUALS,
                            new Literal(115L)))
                    .build())
            .getReviewInstances();
    assertEquals(
        List.of(2_104_705L),
        reviewInstancesByBoth.stream()
            .map(
                ri ->
                    ri.getAttributeValues()
                        .get(primaryEntity.getIdAttribute())
                        .getValue()
                        .getInt64Val())
            .collect(Collectors.toList()));
  }

  private void checkEntityInstances(
      List<Long> instanceIds, Attribute idAttribute, List<ReviewInstance> reviewInstances) {
    for (Long instanceId : instanceIds) {
      assertTrue(
          reviewInstances.stream()
              .filter(
                  ri ->
                      ri.getAttributeValues()
                          .get(idAttribute)
                          .getValue()
                          .equals(new Literal(instanceId)))
              .findFirst()
              .isPresent());
    }
  }

  private void checkAttributeValue(
      Attribute attribute,
      Long expectedAttributeValue,
      Long instanceId,
      Attribute idAttribute,
      List<ReviewInstance> reviewInstances) {
    ReviewInstance reviewInstance =
        reviewInstances.stream()
            .filter(
                ri ->
                    ri.getAttributeValues()
                        .get(idAttribute)
                        .getValue()
                        .getInt64Val()
                        .equals(instanceId))
            .findFirst()
            .get();
    ValueDisplay actualAttributeValue = reviewInstance.getAttributeValues().get(attribute);
    assertNotNull(actualAttributeValue);
    assertEquals(expectedAttributeValue, actualAttributeValue.getValue().getInt64Val());
  }

  private void checkAnnotationValues(
      List<AnnotationValue> annotationValues,
      Long instanceId,
      Attribute idAttribute,
      List<ReviewInstance> reviewInstances) {
    ReviewInstance reviewInstance =
        reviewInstances.stream()
            .filter(
                ri ->
                    ri.getAttributeValues()
                        .get(idAttribute)
                        .getValue()
                        .getInt64Val()
                        .equals(instanceId))
            .findFirst()
            .get();
    assertEquals(annotationValues.size(), reviewInstance.getAnnotationValues().size());
    for (AnnotationValue annotationValue : annotationValues) {
      assertTrue(reviewInstance.getAnnotationValues().contains(annotationValue));
    }
  }
}
