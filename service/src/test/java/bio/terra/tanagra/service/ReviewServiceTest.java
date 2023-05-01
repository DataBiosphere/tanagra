package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_1;
import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_2;
import static bio.terra.tanagra.service.CriteriaValues.*;
import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.NotFoundException;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.inmemory.InMemoryRowResult;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceIdCollection;
import bio.terra.tanagra.service.model.Cohort;
import bio.terra.tanagra.service.model.Review;
import bio.terra.tanagra.service.model.Study;
import java.util.Collections;
import java.util.List;
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
public class ReviewServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewServiceTest.class);
  private static final String UNDERLAY_NAME = "cms_synpuf";

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
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
  void createUpdateDelete() throws InterruptedException {
    // Create.
    String displayName = "review 1";
    String description = "first review";
    String createdByEmail = "abc@123.com";
    ColumnHeaderSchema columnHeaderSchema =
        new ColumnHeaderSchema(List.of(new ColumnSchema("id", CellValue.SQLDataType.INT64)));
    QueryResult queryResult =
        new QueryResult(
            List.of(123L, 456L, 789L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    Review createdReview =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort1.getId(),
            Review.builder().displayName(displayName).description(description).size(11),
            createdByEmail,
            queryResult);
    assertNotNull(createdReview);
    LOGGER.info("Created review {} at {}", createdReview.getId(), createdReview.getCreated());
    assertEquals(11, createdReview.getSize());
    assertEquals(displayName, createdReview.getDisplayName());
    assertEquals(description, createdReview.getDescription());
    assertEquals(createdByEmail, createdReview.getCreatedBy());
    assertEquals(createdByEmail, createdReview.getLastModifiedBy());
    assertEquals(createdReview.getCreated(), createdReview.getLastModified());
    assertFalse(createdReview.getRevision().isEditable());
    assertFalse(createdReview.getRevision().isMostRecent());
    assertEquals(
        cohort1.getMostRecentRevision().getSections(), createdReview.getRevision().getSections());

    // Update.
    TimeUnit.SECONDS.sleep(1); // Wait briefly, so the last modified and created timestamps differ.
    String displayName2 = "review 1 updated";
    String description2 = "first review updated";
    String updatedByEmail = "efg@123.com";
    Review updatedReview =
        reviewService.updateReview(
            study1.getId(),
            cohort1.getId(),
            createdReview.getId(),
            updatedByEmail,
            displayName2,
            description2);
    assertNotNull(updatedReview);
    LOGGER.info("Updated review {} at {}", updatedReview.getId(), updatedReview.getLastModified());
    assertEquals(displayName2, updatedReview.getDisplayName());
    assertEquals(description2, updatedReview.getDescription());
    assertEquals(createdByEmail, updatedReview.getCreatedBy());
    assertEquals(updatedByEmail, updatedReview.getLastModifiedBy());
    assertTrue(updatedReview.getLastModified().isAfter(updatedReview.getCreated()));

    // Delete.
    reviewService.deleteReview(study1.getId(), cohort1.getId(), createdReview.getId());
    assertThrows(
        NotFoundException.class,
        () -> reviewService.getReview(study1.getId(), cohort1.getId(), createdReview.getId()));
  }

  @Test
  void listAllOrSelected() {
    String userEmail = "abc@123.com";
    ColumnHeaderSchema columnHeaderSchema =
        new ColumnHeaderSchema(List.of(new ColumnSchema("id", CellValue.SQLDataType.INT64)));
    QueryResult queryResult =
        new QueryResult(
            List.of(123L, 456L, 789L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);

    // Create one review for cohort1.
    Review review1 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort1.getId(),
            Review.builder().displayName("review 1").description("first review").size(11),
            userEmail,
            queryResult);
    assertNotNull(review1);
    LOGGER.info("Created review {} at {}", review1.getId(), review1.getCreated());

    // Create two reviews for cohort2.
    Review review2 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort2.getId(),
            Review.builder().displayName("review 2").description("second review").size(3),
            userEmail,
            queryResult);
    assertNotNull(review2);
    LOGGER.info("Created review {} at {}", review2.getId(), review2.getCreated());
    Review review3 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort2.getId(),
            Review.builder().displayName("review 3").description("third review").size(5),
            userEmail,
            queryResult);
    assertNotNull(review3);
    LOGGER.info("Created review {} at {}", review3.getId(), review3.getCreated());

    // List all reviews for cohort2.
    List<Review> allReviews =
        reviewService.listReviews(
            ResourceIdCollection.allResourceIds(), study1.getId(), cohort2.getId(), 0, 10);
    assertEquals(2, allReviews.size());
    LOGGER.info("reviews found: {}, {}", allReviews.get(0).getId(), allReviews.get(1).getId());

    // List selected review for cohort2.
    List<Review> selectedReviews =
        reviewService.listReviews(
            ResourceIdCollection.forCollection(List.of(new ResourceId(review3.getId()))),
            study1.getId(),
            cohort2.getId(),
            0,
            10);
    assertEquals(1, selectedReviews.size());
  }

  @Test
  void invalid() {
    // List all.
    List<Review> allReviews =
        reviewService.listReviews(
            ResourceIdCollection.allResourceIds(), study1.getId(), cohort1.getId(), 0, 10);
    assertTrue(allReviews.isEmpty());

    // List selected.
    List<Review> selectedReviews =
        reviewService.listReviews(
            ResourceIdCollection.forCollection(List.of(new ResourceId("123"))),
            study1.getId(),
            cohort1.getId(),
            0,
            10);
    assertTrue(selectedReviews.isEmpty());

    // Get invalid review.
    assertThrows(NotFoundException.class, () -> reviewService.getReview("789", "123", "456"));
    assertThrows(
        NotFoundException.class,
        () -> reviewService.getReview(study1.getId(), cohort1.getId(), "123"));

    // Specify empty query result.
    ColumnHeaderSchema columnHeaderSchema =
        new ColumnHeaderSchema(List.of(new ColumnSchema("id", CellValue.SQLDataType.INT64)));
    QueryResult emptyQueryResult = new QueryResult(Collections.emptyList(), columnHeaderSchema);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            reviewService.createReviewHelper(
                study1.getId(),
                cohort1.getId(),
                Review.builder().size(11),
                "abc@123.com",
                emptyQueryResult));
  }
}
