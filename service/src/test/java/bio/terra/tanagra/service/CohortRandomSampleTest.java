package bio.terra.tanagra.service;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_1;
import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.filter.EntityFilter;
import bio.terra.tanagra.api.query.list.ListQueryRequest;
import bio.terra.tanagra.api.shared.BinaryOperator;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.Study;
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
public class CohortRandomSampleTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(CohortRandomSampleTest.class);
  private static final String UNDERLAY_NAME = "cmssynpuf";

  @Autowired private StudyService studyService;
  @Autowired private CohortService cohortService;
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
            List.of(CRITERIA_GROUP_SECTION_1, CRITERIA_GROUP_SECTION_2));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort1 {} at {}", cohort1.getId(), cohort1.getCreated());
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
  void singlePage() {
    List<Long> randomSample = getRandomSample(ListQueryRequest.DEFAULT_PAGE_SIZE - 1);
    assertEquals(ListQueryRequest.DEFAULT_PAGE_SIZE - 1, randomSample.size());
  }

  @Test
  void multiplePages() {
    List<Long> randomSample = getRandomSample(ListQueryRequest.DEFAULT_PAGE_SIZE * 2 + 1);
    assertEquals(ListQueryRequest.DEFAULT_PAGE_SIZE * 2 + 1, randomSample.size());
  }

  private List<Long> getRandomSample(int sampleSize) {
    Underlay underlay = underlayService.getUnderlay(UNDERLAY_NAME);
    Entity primaryEntity = underlay.getPrimaryEntity();
    EntityFilter cohortEntityFilter =
        new AttributeFilter(
            underlay,
            primaryEntity,
            primaryEntity.getAttribute("gender"),
            BinaryOperator.EQUALS,
            Literal.forInt64(8_532L));
    return cohortService.getRandomSample(
        study1.getId(), cohort1.getId(), cohortEntityFilter, sampleSize);
  }
}
