package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_GENDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.exception.InvalidQueryException;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewInstance;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryRequest;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryResult;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
public class ReviewSampleSizeTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewSampleSizeTest.class);
  private static final String UNDERLAY_NAME = "cmssynpuf";

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
  @Autowired private ReviewService reviewService;
  @Autowired private UnderlayService underlayService;

  private Study study1;
  private Cohort cohort1;

  @BeforeEach
  void createStudyAndCohort() {
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
            List.of(CRITERIA_GROUP_SECTION_GENDER));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort1 {} at {}", cohort1.getId(), cohort1.getCreated());
  }

  @AfterEach
  void deleteStudy() {
    try {
      studyService.deleteStudy(study1.getId(), "abc@123.com");
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }
  }

  @Test
  void lessThanOneDefaultPageSize() {
    List<Long> randomSample =
        cohortService.getRandomSample(
            study1.getId(),
            cohort1.getId(),
            ListQueryRequest.DEFAULT_PAGE_SIZE - 1,
            getCohortFilter());
    assertEquals(ListQueryRequest.DEFAULT_PAGE_SIZE - 1, randomSample.size());
  }

  @Test
  void multipleDefaultPageSizes() {
    List<Long> randomSample =
        cohortService.getRandomSample(
            study1.getId(),
            cohort1.getId(),
            ListQueryRequest.DEFAULT_PAGE_SIZE * 2 + 1,
            getCohortFilter());
    assertEquals(ListQueryRequest.DEFAULT_PAGE_SIZE * 2 + 1, randomSample.size());
  }

  @Test
  void maxReviewSize() {
    Review review1 =
        reviewService.createReview(
            study1.getId(),
            cohort1.getId(),
            Review.builder().size(ReviewService.MAX_REVIEW_SIZE),
            "abc@123.com",
            getCohortFilter());
    assertNotNull(review1);
    LOGGER.info("Created review {} at {}", review1.getId(), review1.getCreated());
    assertEquals(ReviewService.MAX_REVIEW_SIZE, review1.getSize());

    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    ReviewQueryRequest reviewQueryRequest =
        ReviewQueryRequest.builder()
            .attributes(List.of(underlay.getPrimaryEntity().getIdAttribute()))
            .pageMarker(null)
            .build();
    ReviewQueryResult reviewQueryResult =
        reviewService.listReviewInstances(
            study1.getId(), cohort1.getId(), review1.getId(), reviewQueryRequest);
    List<ReviewInstance> reviewInstances = new ArrayList<>();
    reviewQueryResult.getReviewInstances().stream()
        .forEach(reviewInstance -> reviewInstances.add(reviewInstance));

    // Paginate through results.
    while (reviewQueryResult.getPageMarker() != null) {
      reviewQueryRequest =
          ReviewQueryRequest.builder()
              .attributes(List.of(underlay.getPrimaryEntity().getIdAttribute()))
              .pageMarker(reviewQueryResult.getPageMarker())
              .pageSize(ReviewService.MAX_REVIEW_SIZE)
              .build();
      reviewQueryResult =
          reviewService.listReviewInstances(
              study1.getId(), cohort1.getId(), review1.getId(), reviewQueryRequest);
      reviewQueryResult.getReviewInstances().stream()
          .forEach(reviewInstance -> reviewInstances.add(reviewInstance));
    }
    assertEquals(ReviewService.MAX_REVIEW_SIZE, reviewInstances.size());

    // Make sure all the ids are unique (i.e. we're not just fetching the same page over and over).
    Set<Long> primaryEntityIds = new HashSet<>();
    reviewInstances.stream()
        .forEach(
            reviewInstance ->
                primaryEntityIds.add(
                    reviewInstance
                        .getAttributeValues()
                        .get(underlay.getPrimaryEntity().getIdAttribute())
                        .getValue()
                        .getInt64Val()));
    assertEquals(ReviewService.MAX_REVIEW_SIZE, primaryEntityIds.size());
  }

  @Test
  void greaterThanMaxReviewSize() {
    assertThrows(
        InvalidQueryException.class,
        () ->
            reviewService.createReview(
                study1.getId(),
                cohort1.getId(),
                Review.builder().size(ReviewService.MAX_REVIEW_SIZE + 1),
                "abc@123.com",
                getCohortFilter()));
  }

  private EntityFilter getCohortFilter() {
    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    Entity primaryEntity = underlay.getPrimaryEntity();
    return new AttributeFilter(
        underlay,
        primaryEntity,
        primaryEntity.getAttribute("gender"),
        BinaryOperator.EQUALS,
        Literal.forInt64(8_532L));
  }
}
