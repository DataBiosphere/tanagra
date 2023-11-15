package bio.terra.tanagra.service.accesscontrol;

import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_1;
import static bio.terra.tanagra.service.CriteriaGroupSectionValues.CRITERIA_GROUP_SECTION_2;
import static bio.terra.tanagra.service.CriteriaValues.ETHNICITY_EQ_JAPANESE;
import static bio.terra.tanagra.service.CriteriaValues.PROCEDURE_EQ_AMPUTATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.inmemory.InMemoryRowResult;
import bio.terra.tanagra.service.UnderlayService;
import bio.terra.tanagra.service.artifact.AnnotationService;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ConceptSetService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.artifact.model.AnnotationKey;
import bio.terra.tanagra.service.artifact.model.Cohort;
import bio.terra.tanagra.service.artifact.model.ConceptSet;
import bio.terra.tanagra.service.artifact.model.Review;
import bio.terra.tanagra.service.artifact.model.Study;
import bio.terra.tanagra.service.authentication.UserId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
@SuppressWarnings("PMD.TooManyFields")
public class BaseAccessControlTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseAccessControlTest.class);
  @Autowired protected UnderlayService underlayService;
  @Autowired protected StudyService studyService;
  @Autowired protected CohortService cohortService;
  @Autowired protected ConceptSetService conceptSetService;
  @Autowired protected ReviewService reviewService;
  @Autowired protected AnnotationService annotationService;

  protected AccessControl impl;
  protected static final String CMS_SYNPUF = "cmssynpuf";
  protected static final String AOU_SYNTHETIC = "aouSR2019q4r4";
  protected static final String SDD = "sd020230331";

  protected static final UserId USER_1 = UserId.fromToken("subject1", "user1@gmail.com", "token1");
  protected static final UserId USER_2 = UserId.fromToken("subject2", "user2@gmail.com", "token2");
  protected static final UserId USER_3 = UserId.fromToken("subject3", "user3@gmail.com", "token3");
  protected static final UserId USER_4 = UserId.fromToken("subject4", "user4@gmail.com", "token4");

  protected Study study1;
  protected Study study2;
  protected Cohort cohort1;
  protected Cohort cohort2;
  protected ConceptSet conceptSet1;
  protected ConceptSet conceptSet2;
  protected Review review1;
  protected Review review2;
  protected AnnotationKey annotationKey1;
  protected AnnotationKey annotationKey2;

  protected void createArtifacts() {
    // Create 2 studies.
    study1 = studyService.createStudy(Study.builder().displayName("study 1"), "abc@123.com");
    assertNotNull(study1);
    LOGGER.info("Created study1 {} at {}", study1.getId(), study1.getCreated());

    study2 = studyService.createStudy(Study.builder().displayName("study 2"), "def@123.com");
    assertNotNull(study2);
    LOGGER.info("Created study2 {} at {}", study2.getId(), study2.getCreated());

    // Create 2 cohorts.
    cohort1 =
        cohortService.createCohort(
            study1.getId(),
            Cohort.builder()
                .underlay(CMS_SYNPUF)
                .displayName("cohort 2")
                .description("first cohort"),
            "abc@123.com",
            List.of(CRITERIA_GROUP_SECTION_1, CRITERIA_GROUP_SECTION_2));
    assertNotNull(cohort1);
    LOGGER.info("Created cohort {} at {}", cohort1.getId(), cohort1.getCreated());

    cohort2 =
        cohortService.createCohort(
            study2.getId(),
            Cohort.builder()
                .underlay(CMS_SYNPUF)
                .displayName("cohort 2")
                .description("second cohort"),
            "def@123.com",
            List.of(CRITERIA_GROUP_SECTION_2));
    assertNotNull(cohort2);
    LOGGER.info("Created cohort {} at {}", cohort2.getId(), cohort2.getCreated());

    // Create 2 concept sets.
    conceptSet1 =
        conceptSetService.createConceptSet(
            study1.getId(),
            ConceptSet.builder()
                .underlay(CMS_SYNPUF)
                .displayName("concept set 1")
                .description("first concept set")
                .entity(ETHNICITY_EQ_JAPANESE.getKey())
                .criteria(List.of(ETHNICITY_EQ_JAPANESE.getValue())),
            "abc@123.com");
    assertNotNull(conceptSet1);
    LOGGER.info("Created concept set {} at {}", conceptSet1.getId(), conceptSet1.getCreated());

    conceptSet2 =
        conceptSetService.createConceptSet(
            study2.getId(),
            ConceptSet.builder()
                .underlay(CMS_SYNPUF)
                .displayName("concept set 2")
                .description("second concept set")
                .entity(PROCEDURE_EQ_AMPUTATION.getKey())
                .criteria(List.of(PROCEDURE_EQ_AMPUTATION.getValue())),
            "def@123.com");
    assertNotNull(conceptSet2);
    LOGGER.info("Created concept set {} at {}", conceptSet2.getId(), conceptSet2.getCreated());

    // Create 2 reviews.
    ColumnHeaderSchema columnHeaderSchema =
        new ColumnHeaderSchema(List.of(new ColumnSchema("id", CellValue.SQLDataType.INT64)));
    QueryResult queryResult =
        new QueryResult(
            List.of(123L, 456L, 789L).stream()
                .map(id -> new InMemoryRowResult(List.of(id), columnHeaderSchema))
                .collect(Collectors.toList()),
            columnHeaderSchema);
    review1 =
        reviewService.createReviewHelper(
            study1.getId(),
            cohort1.getId(),
            Review.builder().displayName("review 1").description("first review").size(11),
            "abc@123.com",
            queryResult,
            14);
    assertNotNull(review1);
    LOGGER.info("Created review {} at {}", review1.getId(), review1.getCreated());
    review2 =
        reviewService.createReviewHelper(
            study2.getId(),
            cohort2.getId(),
            Review.builder().displayName("review 2").description("second review").size(3),
            "def@123.com",
            queryResult,
            15);
    assertNotNull(review2);
    LOGGER.info("Created review {} at {}", review2.getId(), review2.getCreated());

    // Create 2 annotation keys.
    annotationKey1 =
        annotationService.createAnnotationKey(
            study1.getId(),
            cohort1.getId(),
            AnnotationKey.builder()
                .displayName("annotation key 1")
                .description("first annotation key")
                .dataType(Literal.DataType.BOOLEAN));
    assertNotNull(annotationKey1);
    LOGGER.info("Created annotation key {}", annotationKey1.getId());
    annotationKey2 =
        annotationService.createAnnotationKey(
            study2.getId(),
            cohort2.getId(),
            AnnotationKey.builder()
                .displayName("annotation key 2")
                .description("second annotation key")
                .dataType(Literal.DataType.INT64));
    assertNotNull(annotationKey2);
    LOGGER.info("Created annotation key {}", annotationKey2.getId());
  }

  protected void deleteStudies() {
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

  protected void assertHasPermissions(UserId user, ResourceId resource, Action... actions) {
    Action[] actionsArr =
        actions.length > 0 ? actions : resource.getType().getActions().toArray(new Action[0]);
    assertTrue(
        impl.isAuthorized(user, Permissions.forActions(resource.getType(), actionsArr), resource));
    assertTrue(
        impl.getPermissions(user, resource)
            .contains(Permissions.forActions(resource.getType(), actionsArr)));

    ResourceCollection resources =
        impl.listAllPermissions(
            user, resource.getType(), resource.getParent(), 0, Integer.MAX_VALUE);
    assertTrue(
        resources
            .getPermissions(resource)
            .contains(Permissions.forActions(resource.getType(), actionsArr)));

    if (new HashSet<>(Arrays.asList(actionsArr)).equals(resource.getType().getActions())) {
      assertTrue(impl.getPermissions(user, resource).isAllActions());
      assertTrue(resources.getPermissions(resource).isAllActions());
    }

    resources =
        impl.listAuthorizedResources(
            user,
            Permissions.forActions(resource.getType(), actionsArr),
            resource.getParent(),
            0,
            Integer.MAX_VALUE);
    assertTrue(resources.contains(resource));
  }

  protected void assertDoesNotHavePermissions(UserId user, ResourceId resource, Action... actions) {
    Action[] actionsArr =
        actions.length > 0 ? actions : resource.getType().getActions().toArray(new Action[0]);
    assertFalse(
        impl.isAuthorized(user, Permissions.forActions(resource.getType(), actionsArr), resource));
    assertFalse(
        impl.getPermissions(user, resource)
            .contains(Permissions.forActions(resource.getType(), actionsArr)));

    ResourceCollection resources =
        impl.listAllPermissions(
            user, resource.getType(), resource.getParent(), 0, Integer.MAX_VALUE);
    assertFalse(
        resources
            .getPermissions(resource)
            .contains(Permissions.forActions(resource.getType(), actionsArr)));

    if (new HashSet<>(Arrays.asList(actionsArr)).equals(resource.getType().getActions())) {
      assertTrue(impl.getPermissions(user, resource).isEmpty());
      assertTrue(resources.getPermissions(resource).isEmpty());
    }

    resources =
        impl.listAuthorizedResources(
            user,
            Permissions.forActions(resource.getType(), actionsArr),
            resource.getParent(),
            0,
            Integer.MAX_VALUE);
    assertFalse(resources.contains(resource));
  }

  protected void assertServiceListWithReadPermission(
      UserId user,
      ResourceType type,
      ResourceId parent,
      boolean isAllResources,
      ResourceId... expectedResources) {
    ResourceCollection resources =
        impl.listAuthorizedResources(
            user, Permissions.forActions(type, Action.READ), parent, 0, Integer.MAX_VALUE);
    assertEquals(isAllResources, resources.isAllResources());

    Set<ResourceId> actual;
    switch (type) {
      case UNDERLAY:
        actual =
            underlayService.listUnderlays(resources).stream()
                .map(u -> ResourceId.forUnderlay(u.getName()))
                .collect(Collectors.toSet());
        break;
      case STUDY:
        actual =
            studyService.listStudies(resources, 0, Integer.MAX_VALUE).stream()
                .map(s -> ResourceId.forStudy(s.getId()))
                .collect(Collectors.toSet());
        break;
      case COHORT:
        actual =
            cohortService.listCohorts(resources, 0, Integer.MAX_VALUE).stream()
                .map(c -> ResourceId.forCohort(parent.getStudy(), c.getId()))
                .collect(Collectors.toSet());
        break;
      case CONCEPT_SET:
        actual =
            conceptSetService.listConceptSets(resources, 0, Integer.MAX_VALUE).stream()
                .map(c -> ResourceId.forConceptSet(parent.getStudy(), c.getId()))
                .collect(Collectors.toSet());
        break;
      case REVIEW:
        actual =
            reviewService.listReviews(resources, 0, Integer.MAX_VALUE).stream()
                .map(r -> ResourceId.forReview(parent.getStudy(), parent.getCohort(), r.getId()))
                .collect(Collectors.toSet());
        break;
      case ANNOTATION_KEY:
        actual =
            annotationService.listAnnotationKeys(resources, 0, Integer.MAX_VALUE).stream()
                .map(
                    a ->
                        ResourceId.forAnnotationKey(
                            parent.getStudy(), parent.getCohort(), a.getId()))
                .collect(Collectors.toSet());
        break;
      default:
        throw new IllegalArgumentException("Unknown resource type: " + type);
    }
    List<ResourceId> expected = Arrays.asList(expectedResources);
    assertEquals(expected.size(), actual.size());
    actual.stream().forEach(r -> assertTrue(expected.contains(r)));
  }
}
