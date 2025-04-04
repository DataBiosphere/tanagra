package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.criteriaconstants.cmssynpuf.CriteriaGroupSection.CRITERIA_GROUP_SECTION_GENDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.api.shared.OrderByDirection;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryOrderBy;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryRequest;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryResult;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import java.util.List;
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
public class ReviewPaginationTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReviewPaginationTest.class);
  private static final String UNDERLAY_NAME = "cmssynpuf";
  private static final String USER_EMAIL_1 = "abc@123.com";
  @Autowired private UnderlayService underlayService;
  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
  @Autowired private ReviewService reviewService;
  private Study study1;
  private Cohort cohort1;
  private Review review1;

  @BeforeEach
  void createReview() {
    study1 = studyService.createStudy(Study.builder(), USER_EMAIL_1);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder().underlay(UNDERLAY_NAME).createdBy(USER_EMAIL_1),
            List.of(CRITERIA_GROUP_SECTION_GENDER));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    Entity primaryEntity = underlay.getPrimaryEntity();
    review1 =
        reviewService.createReview(
            study1.getId(),
            cohort1.getId(),
            Review.builder().size(10),
            USER_EMAIL_1,
            new AttributeFilter(
                underlay,
                primaryEntity,
                primaryEntity.getAttribute("gender"),
                BinaryOperator.EQUALS,
                Literal.forInt64(8_532L)));
    assertNotNull(review1);
    LOGGER.info("Created review {} at {}", review1.getId(), review1.getCreated());
  }

  @AfterEach
  void deleteStudy() {
    try {
      studyService.deleteStudy(study1.getId(), USER_EMAIL_1);
      LOGGER.info("Deleted study1 {}", study1.getId());
    } catch (Exception ex) {
      LOGGER.error("Error deleting study1", ex);
    }
  }

  @Test
  void noPagination() {
    Entity primaryEntity = underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();
    ReviewQueryRequest reviewQueryRequest =
        ReviewQueryRequest.builder()
            .attributes(primaryEntity.getAttributes())
            .orderBys(
                List.of(
                    new ReviewQueryOrderBy(
                        primaryEntity.getIdAttribute(), OrderByDirection.DESCENDING)))
            .build();
    ReviewQueryResult reviewQueryResult =
        reviewService.listReviewInstances(
            study1.getId(), cohort1.getId(), review1.getId(), reviewQueryRequest);
    assertNotNull(reviewQueryResult.sql());
    assertEquals(10, reviewQueryResult.reviewInstances().size());
    assertNull(reviewQueryResult.pageMarker());
  }

  @Test
  void withPagination() {
    Entity primaryEntity = underlayService.getUnderlay(UNDERLAY_NAME).getPrimaryEntity();

    // First query request gets the first page of results.
    ReviewQueryRequest reviewQueryRequest1 =
        ReviewQueryRequest.builder()
            .attributes(primaryEntity.getAttributes())
            .orderBys(
                List.of(
                    new ReviewQueryOrderBy(
                        primaryEntity.getIdAttribute(), OrderByDirection.DESCENDING)))
            .pageSize(4)
            .build();
    ReviewQueryResult reviewQueryResult1 =
        reviewService.listReviewInstances(
            study1.getId(), cohort1.getId(), review1.getId(), reviewQueryRequest1);

    assertNotNull(reviewQueryResult1.sql());
    assertEquals(4, reviewQueryResult1.reviewInstances().size());
    assertNotNull(reviewQueryResult1.pageMarker());
    assertNotNull(reviewQueryResult1.pageMarker().getOffset());

    // Second query request gets the second and final page of results.
    ReviewQueryRequest reviewQueryRequest2 =
        ReviewQueryRequest.builder()
            .attributes(primaryEntity.getAttributes())
            .orderBys(
                List.of(
                    new ReviewQueryOrderBy(
                        primaryEntity.getIdAttribute(), OrderByDirection.DESCENDING)))
            .pageSize(8)
            .pageMarker(reviewQueryResult1.pageMarker())
            .build();
    ReviewQueryResult reviewQueryResult2 =
        reviewService.listReviewInstances(
            study1.getId(), cohort1.getId(), review1.getId(), reviewQueryRequest2);

    assertNotNull(reviewQueryResult2.sql());
    assertEquals(6, reviewQueryResult2.reviewInstances().size());
    assertNull(reviewQueryResult2.pageMarker());
  }
}
