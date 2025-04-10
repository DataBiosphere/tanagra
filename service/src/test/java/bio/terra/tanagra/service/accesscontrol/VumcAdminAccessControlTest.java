package bio.terra.tanagra.service.accesscontrol;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.service.accesscontrol.impl.MockVumcAdminAccessControl;
import bio.terra.tanagra.service.authentication.UserId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vumc.vda.tanagra.admin.model.ResourceAction;

public class VumcAdminAccessControlTest extends BaseAccessControlTest {
  @BeforeEach
  void createArtifactsAndDefinePermissionsInMock() {
    createArtifacts();

    // Define user permissions in VumcAdmin mock impl.
    MockVumcAdminAccessControl vaImpl = new MockVumcAdminAccessControl();
    // activity logs
    //   user1: READ
    //   user2: READ
    //   user3:
    //   user4:
    vaImpl.addPermission(
        USER_1,
        ResourceAction.READ,
        org.vumc.vda.tanagra.admin.model.ResourceType.ACTIVITY_LOG,
        null);
    vaImpl.addPermission(
        USER_2,
        ResourceAction.READ,
        org.vumc.vda.tanagra.admin.model.ResourceType.ACTIVITY_LOG,
        null);
    // underlays
    //   user1: cmssynpuf (ALL)
    //   user2: cmssynpuf (READ), aousynthetic (ALL)
    //   user3:
    //   user4: cmssynpuf (READ), aousynthetic (READ), sdd (ALL)
    vaImpl.addPermission(
        USER_1,
        ResourceAction.ALL,
        org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY,
        CMS_SYNPUF);
    vaImpl.addPermission(
        USER_2,
        ResourceAction.READ,
        org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY,
        CMS_SYNPUF);
    vaImpl.addPermission(
        USER_2,
        ResourceAction.ALL,
        org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY,
        AOU_SYNTHETIC);
    vaImpl.addPermission(
        USER_4,
        ResourceAction.READ,
        org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY,
        CMS_SYNPUF);
    vaImpl.addPermission(
        USER_4,
        ResourceAction.READ,
        org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY,
        AOU_SYNTHETIC);
    vaImpl.addPermission(
        USER_4, ResourceAction.READ, org.vumc.vda.tanagra.admin.model.ResourceType.UNDERLAY, SDD);
    // studies
    //   user1: CREATE (=isAdmin), study1 (UPDATE, DELETE)
    //   user2: CREATE (=isAdmin), study1 (ALL), study2 (READ, UPDATE, DELETE)
    //   user3:
    //   user4: study2 (READ)
    vaImpl.addAdminUser(USER_1);
    vaImpl.addPermission(
        USER_1,
        ResourceAction.UPDATE,
        org.vumc.vda.tanagra.admin.model.ResourceType.STUDY,
        study1.getId());
    vaImpl.addPermission(
        USER_1,
        ResourceAction.DELETE,
        org.vumc.vda.tanagra.admin.model.ResourceType.STUDY,
        study1.getId());
    vaImpl.addAdminUser(USER_2);
    vaImpl.addPermission(
        USER_2,
        ResourceAction.ALL,
        org.vumc.vda.tanagra.admin.model.ResourceType.STUDY,
        study1.getId());
    vaImpl.addPermission(
        USER_2,
        ResourceAction.READ,
        org.vumc.vda.tanagra.admin.model.ResourceType.STUDY,
        study2.getId());
    vaImpl.addPermission(
        USER_2, ResourceAction.CREATE, org.vumc.vda.tanagra.admin.model.ResourceType.STUDY, null);
    vaImpl.addPermission(
        USER_2,
        ResourceAction.UPDATE,
        org.vumc.vda.tanagra.admin.model.ResourceType.STUDY,
        study2.getId());
    vaImpl.addPermission(
        USER_2,
        ResourceAction.DELETE,
        org.vumc.vda.tanagra.admin.model.ResourceType.STUDY,
        study2.getId());
    vaImpl.addPermission(
        USER_4,
        ResourceAction.READ,
        org.vumc.vda.tanagra.admin.model.ResourceType.STUDY,
        study2.getId());

    AccessControlConfiguration accessControlConfig = new AccessControlConfiguration();
    accessControlConfig.setParams(List.of());
    accessControlConfig.setBasePath("FAKE_BASE_PATH");
    accessControlConfig.setOauthClientId("FAKE_OAUTH_CLIENT_ID");
    accessControlService = new AccessControlService(vaImpl, accessControlConfig, studyService);
  }

  @AfterEach
  protected void cleanup() {
    deleteStudies();
  }

  @Test
  void activityLog() {
    // isAuthorized
    assertTrue(
        accessControlService.isAuthorized(
            USER_1, Permissions.allActions(ResourceType.ACTIVITY_LOG)));
    assertTrue(
        accessControlService.isAuthorized(
            USER_2, Permissions.allActions(ResourceType.ACTIVITY_LOG)));
    assertFalse(
        accessControlService.isAuthorized(
            USER_3, Permissions.allActions(ResourceType.ACTIVITY_LOG)));
    assertFalse(
        accessControlService.isAuthorized(
            USER_4, Permissions.allActions(ResourceType.ACTIVITY_LOG)));
  }

  @Test
  void underlay() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId cmsSynpufId = ResourceId.forUnderlay(CMS_SYNPUF);
    ResourceId aouSyntheticId = ResourceId.forUnderlay(AOU_SYNTHETIC);
    ResourceId sddId = ResourceId.forUnderlay(SDD);
    assertHasPermissionsUnderlay(USER_1, cmsSynpufId);
    assertDoesNotHavePermissions(USER_1, aouSyntheticId);
    assertDoesNotHavePermissions(USER_1, sddId);
    assertHasPermissionsUnderlay(USER_2, cmsSynpufId);
    assertHasPermissionsUnderlay(USER_2, aouSyntheticId);
    assertDoesNotHavePermissions(USER_2, sddId);
    assertDoesNotHavePermissions(USER_3, cmsSynpufId);
    assertDoesNotHavePermissions(USER_3, aouSyntheticId);
    assertDoesNotHavePermissions(USER_3, sddId);
    assertHasPermissionsUnderlay(USER_4, cmsSynpufId);
    assertHasPermissionsUnderlay(USER_4, aouSyntheticId);
    assertHasPermissionsUnderlay(USER_4, sddId);

    // service.list
    assertServiceListWithReadPermission(USER_1, ResourceType.UNDERLAY, null, false);
    assertServiceListWithReadPermission(USER_2, ResourceType.UNDERLAY, null, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.UNDERLAY, null, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.UNDERLAY, null, false);
  }

  private void assertHasPermissionsUnderlay(UserId user, ResourceId resource, Action... actions) {
    Action[] actionsArr =
        actions.length > 0 ? actions : resource.getType().getActions().toArray(new Action[0]);
    assertTrue(
        accessControlService.isAuthorized(
            user, Permissions.forActions(resource.getType(), actionsArr), resource));
  }

  @Test
  void study() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    Action[] studyActionsExceptCreate = {
      Action.READ, Action.UPDATE, Action.DELETE, Action.CREATE_COHORT, Action.CREATE_FEATURE_SET,
    };
    ResourceId study1Id = ResourceId.forStudy(study1.getId());
    ResourceId study2Id = ResourceId.forStudy(study2.getId());
    assertHasPermissions(USER_1, study1Id, Action.UPDATE, Action.DELETE);
    assertDoesNotHavePermissions(USER_1, study1Id, Action.READ);
    assertDoesNotHavePermissions(USER_1, study2Id, studyActionsExceptCreate);
    assertHasPermissions(USER_2, study1Id, studyActionsExceptCreate);
    assertHasPermissions(USER_2, study2Id, studyActionsExceptCreate);
    assertDoesNotHavePermissions(USER_3, study1Id, studyActionsExceptCreate);
    assertDoesNotHavePermissions(USER_3, study2Id, studyActionsExceptCreate);
    assertDoesNotHavePermissions(USER_4, study1Id, studyActionsExceptCreate);
    assertHasPermissions(USER_4, study2Id, Action.READ);
    assertDoesNotHavePermissions(
        USER_4,
        study2Id,
        Action.UPDATE,
        Action.DELETE,
        Action.CREATE_COHORT,
        Action.CREATE_FEATURE_SET);

    // isAuthorized for STUDY.CREATE
    assertTrue(
        accessControlService.isAuthorized(
            USER_1, Permissions.forActions(ResourceType.STUDY, Action.CREATE)));
    assertTrue(
        accessControlService.isAuthorized(
            USER_2, Permissions.forActions(ResourceType.STUDY, Action.CREATE)));
    assertFalse(
        accessControlService.isAuthorized(
            USER_3, Permissions.forActions(ResourceType.STUDY, Action.CREATE)));
    assertFalse(
        accessControlService.isAuthorized(
            USER_4, Permissions.forActions(ResourceType.STUDY, Action.CREATE)));

    // service.list
    assertServiceListWithReadPermission(USER_1, ResourceType.STUDY, null, false);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.STUDY, null, false, study1Id, study2Id);
    assertServiceListWithReadPermission(USER_3, ResourceType.STUDY, null, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.STUDY, null, false, study2Id);
  }

  @Test
  void cohort() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId cohort1Id = ResourceId.forCohort(study1.getId(), cohort1.getId());
    ResourceId cohort2Id = ResourceId.forCohort(study2.getId(), cohort2.getId());
    assertHasPermissions(
        USER_1,
        cohort1Id,
        Action.READ,
        Action.UPDATE,
        Action.DELETE,
        Action.CREATE_REVIEW,
        Action.CREATE_ANNOTATION_KEY);
    assertDoesNotHavePermissions(USER_1, cohort2Id);
    assertHasPermissions(USER_2, cohort1Id);
    assertHasPermissions(USER_2, cohort2Id);
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
    assertServiceListWithReadPermission(USER_1, ResourceType.COHORT, study2Id, false);
    assertServiceListWithReadPermission(USER_2, ResourceType.COHORT, study1Id, true, cohort1Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.COHORT, study2Id, true, cohort2Id);
    assertServiceListWithReadPermission(USER_3, ResourceType.COHORT, study1Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.COHORT, study2Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.COHORT, study1Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.COHORT, study2Id, true, cohort2Id);
  }

  @Test
  void featureSet() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId featureSet1Id = ResourceId.forFeatureSet(study1.getId(), featureSet1.getId());
    ResourceId featureSet2Id = ResourceId.forFeatureSet(study2.getId(), featureSet2.getId());
    assertHasPermissions(USER_1, featureSet1Id, Action.READ, Action.UPDATE, Action.DELETE);
    assertDoesNotHavePermissions(USER_1, featureSet2Id);
    assertHasPermissions(USER_2, featureSet1Id);
    assertHasPermissions(USER_2, featureSet2Id);
    assertDoesNotHavePermissions(USER_3, featureSet1Id);
    assertDoesNotHavePermissions(USER_3, featureSet2Id);
    assertDoesNotHavePermissions(USER_4, featureSet1Id);
    assertHasPermissions(USER_4, featureSet2Id, Action.READ);
    assertDoesNotHavePermissions(USER_4, featureSet2Id, Action.UPDATE, Action.DELETE);

    // service.list
    ResourceId study1Id = featureSet1Id.getParent();
    ResourceId study2Id = featureSet2Id.getParent();
    assertServiceListWithReadPermission(
        USER_1, ResourceType.FEATURE_SET, study1Id, true, featureSet1Id);
    assertServiceListWithReadPermission(USER_1, ResourceType.FEATURE_SET, study2Id, false);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.FEATURE_SET, study1Id, true, featureSet1Id);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.FEATURE_SET, study2Id, true, featureSet2Id);
    assertServiceListWithReadPermission(USER_3, ResourceType.FEATURE_SET, study1Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.FEATURE_SET, study2Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.FEATURE_SET, study1Id, false);
    assertServiceListWithReadPermission(
        USER_4, ResourceType.FEATURE_SET, study2Id, true, featureSet2Id);
  }

  @Test
  void review() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId review1Id = ResourceId.forReview(study1.getId(), cohort1.getId(), review1.getId());
    ResourceId review2Id = ResourceId.forReview(study2.getId(), cohort2.getId(), review2.getId());
    assertHasPermissions(USER_1, review1Id);
    assertDoesNotHavePermissions(USER_1, review2Id);
    assertHasPermissions(USER_2, review1Id);
    assertHasPermissions(USER_2, review2Id);
    assertDoesNotHavePermissions(USER_3, review1Id);
    assertDoesNotHavePermissions(USER_3, review2Id);
    assertDoesNotHavePermissions(USER_4, review1Id);
    assertHasPermissions(USER_4, review2Id, Action.READ);
    assertDoesNotHavePermissions(USER_4, review2Id, Action.UPDATE, Action.DELETE);

    // service.list
    ResourceId cohort1Id = review1Id.getParent();
    ResourceId cohort2Id = review2Id.getParent();
    assertServiceListWithReadPermission(USER_1, ResourceType.REVIEW, cohort1Id, true, review1Id);
    assertServiceListWithReadPermission(USER_1, ResourceType.REVIEW, cohort2Id, false);
    assertServiceListWithReadPermission(USER_2, ResourceType.REVIEW, cohort1Id, true, review1Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.REVIEW, cohort2Id, true, review2Id);
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
    assertDoesNotHavePermissions(USER_1, annotationKey2Id);
    assertHasPermissions(USER_2, annotationKey1Id);
    assertHasPermissions(USER_2, annotationKey2Id);
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
    assertServiceListWithReadPermission(USER_1, ResourceType.ANNOTATION_KEY, cohort2Id, false);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.ANNOTATION_KEY, cohort1Id, true, annotationKey1Id);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.ANNOTATION_KEY, cohort2Id, true, annotationKey2Id);
    assertServiceListWithReadPermission(USER_3, ResourceType.ANNOTATION_KEY, cohort1Id, false);
    assertServiceListWithReadPermission(USER_3, ResourceType.ANNOTATION_KEY, cohort2Id, false);
    assertServiceListWithReadPermission(USER_4, ResourceType.ANNOTATION_KEY, cohort1Id, false);
    assertServiceListWithReadPermission(
        USER_4, ResourceType.ANNOTATION_KEY, cohort2Id, true, annotationKey2Id);
  }
}
