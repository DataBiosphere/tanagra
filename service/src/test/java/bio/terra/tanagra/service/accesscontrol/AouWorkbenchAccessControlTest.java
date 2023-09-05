package bio.terra.tanagra.service.accesscontrol;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.tanagra.service.accesscontrol.impl.AouWorkbenchAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.MockAouWorkbenchAccessControl;
import bio.terra.tanagra.service.auth.UserId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AouWorkbenchAccessControlTest extends BaseAccessControlTest {

  @BeforeEach
  void createArtifactsAndDefinePermissionsInMock() {
    createArtifacts();

    // Define user permissions in AouWorkbench mock impl.
    MockAouWorkbenchAccessControl awImpl = new MockAouWorkbenchAccessControl();
    // workspaces
    //   user1: study1 (OWNER), study2 (READER)
    //   user2: study1 (WRITER)
    //   user3:
    //   user4: study2 (READER)
    awImpl.addPermission(USER_1, study1.getId(), AouWorkbenchAccessControl.WorkspaceRole.OWNER);
    awImpl.addPermission(USER_1, study2.getId(), AouWorkbenchAccessControl.WorkspaceRole.READER);
    awImpl.addPermission(USER_2, study1.getId(), AouWorkbenchAccessControl.WorkspaceRole.WRITER);
    awImpl.addPermission(USER_4, study2.getId(), AouWorkbenchAccessControl.WorkspaceRole.READER);

    impl = awImpl;
    impl.initialize(List.of(), "FAKE_BASE_PATH", "FAKE_OAUTH_CLIENT_ID");
  }

  @AfterEach
  protected void cleanup() {
    deleteStudies();
  }

  @Test
  void activityLog() {
    // isAuthorized
    assertFalse(impl.isAuthorized(USER_1, Permissions.allActions(ResourceType.ACTIVITY_LOG), null));
    assertFalse(impl.isAuthorized(USER_2, Permissions.allActions(ResourceType.ACTIVITY_LOG), null));
    assertFalse(impl.isAuthorized(USER_3, Permissions.allActions(ResourceType.ACTIVITY_LOG), null));
    assertFalse(impl.isAuthorized(USER_4, Permissions.allActions(ResourceType.ACTIVITY_LOG), null));
  }

  @Test
  void underlay() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId aouSyntheticId = ResourceId.forUnderlay(AOU_SYNTHETIC);
    assertUnderlayPermissions(USER_1, aouSyntheticId);
    assertUnderlayPermissions(USER_2, aouSyntheticId);
    assertUnderlayPermissions(USER_4, aouSyntheticId);
    // service.list
    assertResourceTypeNoListPermissions(USER_1, ResourceType.UNDERLAY);
    assertResourceTypeNoListPermissions(USER_2, ResourceType.UNDERLAY);
    assertResourceTypeNoListPermissions(USER_4, ResourceType.UNDERLAY);
  }

  @Test
  void study() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    Action[] studyActionsExceptCreate = {
      Action.READ, Action.UPDATE, Action.DELETE, Action.CREATE_COHORT, Action.CREATE_CONCEPT_SET,
    };
    ResourceId study1Id = ResourceId.forStudy(study1.getId());
    ResourceId study2Id = ResourceId.forStudy(study2.getId());
    assertStudyPermissions(USER_1, study1Id, studyActionsExceptCreate);
    assertStudyPermissions(USER_1, study2Id, Action.READ);
    assertDoesNotHavePermissions(
        USER_1,
        study2Id,
        Action.UPDATE,
        Action.DELETE,
        Action.CREATE_COHORT,
        Action.CREATE_CONCEPT_SET);
    assertStudyPermissions(
        USER_2,
        study1Id,
        Action.READ,
        Action.UPDATE,
        Action.CREATE_COHORT,
        Action.CREATE_CONCEPT_SET);
    assertDoesNotHavePermissions(USER_2, study1Id, Action.DELETE);
    assertDoesNotHavePermissions(USER_2, study2Id, studyActionsExceptCreate);
    assertDoesNotHavePermissions(USER_3, study1Id, studyActionsExceptCreate);
    assertDoesNotHavePermissions(USER_3, study2Id, studyActionsExceptCreate);
    assertDoesNotHavePermissions(USER_4, study1Id, studyActionsExceptCreate);
    assertStudyPermissions(USER_4, study2Id, Action.READ);
    assertDoesNotHavePermissions(
        USER_4,
        study2Id,
        Action.UPDATE,
        Action.DELETE,
        Action.CREATE_COHORT,
        Action.CREATE_CONCEPT_SET);

    // isAuthorized for STUDY.CREATE
    assertTrue(
        impl.isAuthorized(USER_1, Permissions.forActions(ResourceType.STUDY, Action.CREATE), null));
    assertTrue(
        impl.isAuthorized(USER_2, Permissions.forActions(ResourceType.STUDY, Action.CREATE), null));
    assertTrue(
        impl.isAuthorized(USER_3, Permissions.forActions(ResourceType.STUDY, Action.CREATE), null));
    assertTrue(
        impl.isAuthorized(USER_4, Permissions.forActions(ResourceType.STUDY, Action.CREATE), null));

    // service.list
    assertResourceTypeNoListPermissions(USER_1, ResourceType.STUDY);
    assertResourceTypeNoListPermissions(USER_2, ResourceType.STUDY);
    assertResourceTypeNoListPermissions(USER_3, ResourceType.STUDY);
    assertResourceTypeNoListPermissions(USER_4, ResourceType.STUDY);
  }

  @Test
  void cohort() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId cohort1Id = ResourceId.forCohort(study1.getId(), cohort1.getId());
    ResourceId cohort2Id = ResourceId.forCohort(study2.getId(), cohort2.getId());
    assertHasPermissions(USER_1, cohort1Id);
    assertHasPermissions(USER_1, cohort2Id, Action.READ);
    assertDoesNotHavePermissions(
        USER_1,
        cohort2Id,
        Action.UPDATE,
        Action.DELETE,
        Action.CREATE_REVIEW,
        Action.CREATE_ANNOTATION_KEY);
    assertHasPermissions(USER_2, cohort1Id);
    assertDoesNotHavePermissions(USER_2, cohort2Id);
    assertDoesNotHavePermissions(USER_3, cohort1Id);
    assertDoesNotHavePermissions(USER_3, cohort2Id);
    assertDoesNotHavePermissions(USER_4, cohort1Id);
    assertHasPermissions(USER_4, cohort2Id, Action.READ);
    assertDoesNotHavePermissions(
        USER_4,
        cohort2Id,
        Action.UPDATE,
        Action.DELETE,
        Action.CREATE_REVIEW,
        Action.CREATE_ANNOTATION_KEY);

    // service.list
    ResourceId study1Id = cohort1Id.getParent();
    ResourceId study2Id = cohort2Id.getParent();
    assertServiceListWithReadPermission(USER_1, ResourceType.COHORT, study1Id, true, cohort1Id);
    assertServiceListWithReadPermission(USER_1, ResourceType.COHORT, study2Id, true, cohort2Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.COHORT, study1Id, true, cohort1Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.COHORT, study2Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.COHORT, study1Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.COHORT, study2Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.COHORT, study1Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.COHORT, study2Id, true, cohort2Id);
  }

  @Test
  void conceptSet() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId conceptSet1Id = ResourceId.forConceptSet(study1.getId(), conceptSet1.getId());
    ResourceId conceptSet2Id = ResourceId.forConceptSet(study2.getId(), conceptSet2.getId());
    assertHasPermissions(USER_1, conceptSet1Id);
    assertHasPermissions(USER_1, conceptSet2Id, Action.READ);
    assertDoesNotHavePermissions(USER_1, conceptSet2Id, Action.UPDATE, Action.DELETE);
    assertHasPermissions(USER_2, conceptSet1Id);
    assertDoesNotHavePermissions(USER_2, conceptSet2Id);
    assertDoesNotHavePermissions(USER_3, conceptSet1Id);
    assertDoesNotHavePermissions(USER_3, conceptSet2Id);
    assertDoesNotHavePermissions(USER_4, conceptSet1Id);
    assertHasPermissions(USER_4, conceptSet2Id, Action.READ);
    assertDoesNotHavePermissions(USER_4, conceptSet2Id, Action.UPDATE, Action.DELETE);

    // service.list
    ResourceId study1Id = conceptSet1Id.getParent();
    ResourceId study2Id = conceptSet2Id.getParent();
    assertServiceListWithReadPermission(
        USER_1, ResourceType.CONCEPT_SET, study1Id, true, conceptSet1Id);
    assertServiceListWithReadPermission(
        USER_1, ResourceType.CONCEPT_SET, study2Id, true, conceptSet2Id);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.CONCEPT_SET, study1Id, true, conceptSet1Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.CONCEPT_SET, study2Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.CONCEPT_SET, study1Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.CONCEPT_SET, study2Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.CONCEPT_SET, study1Id, false);
    assertServiceListWithReadPermission(
        USER_4, ResourceType.CONCEPT_SET, study2Id, true, conceptSet2Id);
  }

  @Test
  void review() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId review1Id = ResourceId.forReview(study1.getId(), cohort1.getId(), review1.getId());
    ResourceId review2Id = ResourceId.forReview(study2.getId(), cohort2.getId(), review2.getId());
    assertHasPermissions(USER_1, review1Id);
    assertHasPermissions(
        USER_1, review2Id, Action.READ, Action.QUERY_INSTANCES, Action.QUERY_COUNTS);
    assertDoesNotHavePermissions(USER_1, review2Id, Action.UPDATE, Action.DELETE);
    assertHasPermissions(USER_2, review1Id);
    assertDoesNotHavePermissions(USER_2, review2Id);
    assertDoesNotHavePermissions(USER_3, review1Id);
    assertDoesNotHavePermissions(USER_3, review2Id);
    assertDoesNotHavePermissions(USER_4, review1Id);
    assertHasPermissions(
        USER_4, review2Id, Action.READ, Action.QUERY_INSTANCES, Action.QUERY_COUNTS);
    assertDoesNotHavePermissions(USER_4, review2Id, Action.UPDATE, Action.DELETE);

    // service.list
    ResourceId cohort1Id = review1Id.getParent();
    ResourceId cohort2Id = review2Id.getParent();
    assertServiceListWithReadPermission(USER_1, ResourceType.REVIEW, cohort1Id, true, review1Id);
    assertServiceListWithReadPermission(USER_1, ResourceType.REVIEW, cohort2Id, true, review2Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.REVIEW, cohort1Id, true, review1Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.REVIEW, cohort2Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.REVIEW, cohort1Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.REVIEW, cohort2Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.REVIEW, cohort1Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.REVIEW, cohort2Id, true, review2Id);
  }

  @Test
  void annotationKey() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId annotationKey1Id =
        ResourceId.forAnnotationKey(study1.getId(), cohort1.getId(), annotationKey1.getId());
    ResourceId annotationKey2Id =
        ResourceId.forAnnotationKey(study2.getId(), cohort2.getId(), annotationKey2.getId());
    assertHasPermissions(USER_1, annotationKey1Id);
    assertHasPermissions(USER_1, annotationKey1Id, Action.READ);
    assertDoesNotHavePermissions(USER_1, annotationKey2Id, Action.UPDATE, Action.DELETE);
    assertHasPermissions(USER_2, annotationKey1Id);
    assertDoesNotHavePermissions(USER_2, annotationKey2Id);
    assertDoesNotHavePermissions(USER_3, annotationKey1Id);
    assertDoesNotHavePermissions(USER_3, annotationKey2Id);
    assertDoesNotHavePermissions(USER_4, annotationKey1Id);
    assertHasPermissions(USER_4, annotationKey2Id, Action.READ);
    assertDoesNotHavePermissions(USER_4, annotationKey2Id, Action.UPDATE, Action.DELETE);

    // service.list
    ResourceId cohort1Id = annotationKey1Id.getParent();
    ResourceId cohort2Id = annotationKey2Id.getParent();
    assertServiceListWithReadPermission(
        USER_1, ResourceType.ANNOTATION_KEY, cohort1Id, true, annotationKey1Id);
    assertServiceListWithReadPermission(
        USER_1, ResourceType.ANNOTATION_KEY, cohort2Id, true, annotationKey2Id);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.ANNOTATION_KEY, cohort1Id, true, annotationKey1Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.ANNOTATION_KEY, cohort2Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.ANNOTATION_KEY, cohort1Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.ANNOTATION_KEY, cohort2Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.ANNOTATION_KEY, cohort1Id, false);
    assertServiceListWithReadPermission(
        USER_4, ResourceType.ANNOTATION_KEY, cohort2Id, true, annotationKey2Id);
  }

  // Specific cases for AoU
  private void assertUnderlayPermissions(UserId user, ResourceId resource) {
    assertTrue(impl.isAuthorized(user, Permissions.empty(resource.getType()), resource));
    assertResourceTypeNoListPermissions(user, resource.getType());
  }

  private void assertStudyPermissions(UserId user, ResourceId resource, Action... actions) {
    assertTrue(impl.isAuthorized(user, Permissions.empty(resource.getType()), resource));
    assertResourceTypeNoListPermissions(user, resource.getType(), actions);
  }

  private void assertResourceTypeNoListPermissions(
      UserId user, ResourceType resource, Action... actions) {
    ResourceCollection resources =
        impl.listAuthorizedResources(
            user,
            Permissions.forActions(resource, resource.getActions()),
            null,
            0,
            Integer.MAX_VALUE);
    assertFalse(resources.isAllResources());
    switch (resource) {
      case UNDERLAY:
        assertEquals(
            actions.length,
            underlaysService.listUnderlays(resources).stream()
                .map(u -> ResourceId.forUnderlay(u.getName()))
                .collect(Collectors.toSet())
                .size());
        break;
      case STUDY:
        assertEquals(
            0,
            studyService.listStudies(resources, 0, Integer.MAX_VALUE).stream()
                .map(u -> ResourceId.forStudy(u.getId()))
                .collect(Collectors.toSet())
                .size());
        break;
      default:
        fail("Method should not be called for ResourceType - " + resource);
    }
  }
}
