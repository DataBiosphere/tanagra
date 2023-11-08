package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import bio.terra.tanagra.api2.filter.AttributeFilter;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByDirection;
import bio.terra.tanagra.query.filtervariable.BinaryFilterVariable;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryOrderBy;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryRequest;
import bio.terra.tanagra.service.artifact.reviewquery.ReviewQueryResult;
import bio.terra.tanagra.underlay2.Underlay;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
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
  @Autowired private UnderlayService underlayService;

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
  @Autowired private ReviewService reviewService;

  private Study study1;
  private Cohort cohort1;
  private Review review1;

  @BeforeEach
  void createReview() {
    String userEmail = "abc@123.com";

    study1 = studyService.createStudy(Study.builder(), userEmail);
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder().underlay(UNDERLAY_NAME),
            userEmail,
            List.of(CRITERIA_GROUP_SECTION_3));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    Entity primaryEntity = underlay.getPrimaryEntity();
    review1 =
        reviewService.createReview(
            study1.getId(),
            cohort1.getId(),
            Review.builder().size(10),
            userEmail,
            new AttributeFilter(
                underlay,
                primaryEntity,
                primaryEntity.getAttribute("gender"),
                BinaryFilterVariable.BinaryOperator.EQUALS,
                new Literal(8532)));
    assertNotNull(review1);
    LOGGER.info("Created review {} at {}", review1.getId(), review1.getCreated());
  }

  @AfterEach
  void deleteReview() {
    try {
      studyService.deleteStudy(study1.getId(), "abc@123.com");
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
    assertNotNull(reviewQueryResult.getSql());
    assertEquals(10, reviewQueryResult.getReviewInstances().size());
    assertNull(reviewQueryResult.getPageMarker());
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

    assertNotNull(reviewQueryResult1.getSql());
    assertEquals(4, reviewQueryResult1.getReviewInstances().size());
    assertNotNull(reviewQueryResult1.getPageMarker());
    assertNotNull(reviewQueryResult1.getPageMarker().getOffset());

    // Second query request gets the second and final page of results.
    ReviewQueryRequest reviewQueryRequest2 =
        ReviewQueryRequest.builder()
            .attributes(primaryEntity.getAttributes())
            .orderBys(
                List.of(
                    new ReviewQueryOrderBy(
                        primaryEntity.getIdAttribute(), OrderByDirection.DESCENDING)))
            .pageSize(8)
            .pageMarker(reviewQueryResult1.getPageMarker())
            .build();
    ReviewQueryResult reviewQueryResult2 =
        reviewService.listReviewInstances(
            study1.getId(), cohort1.getId(), review1.getId(), reviewQueryRequest2);

    assertNotNull(reviewQueryResult2.getSql());
    assertEquals(6, reviewQueryResult2.getReviewInstances().size());
    assertNull(reviewQueryResult2.getPageMarker());
  }
}
