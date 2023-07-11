package bio.terra.tanagra.service.accesscontrol;

import static bio.terra.tanagra.service.accesscontrol.impl.VerilyGroupsAccessControl.ALL_ACCESS;
import static org.junit.jupiter.api.Assertions.*;

import bio.terra.tanagra.service.accesscontrol.impl.MockVerilyGroupsAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.VerilyGroupsAccessControl;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VerilyGroupsAccessControlTest extends BaseAccessControlTest {
  @BeforeEach
  void createArtifactsAndDefinePermissionsInMock() {
    createArtifacts();

    // Define user permissions in VerilyGroups mock impl.
    MockVerilyGroupsAccessControl vgImpl = new MockVerilyGroupsAccessControl();
    // underlays
    //   user1: cmssynpuf
    //   user2: cmssynpuf, aousynthetic
    //   user3:
    //   user4: all
    vgImpl.addGroup(
        new VerilyGroupsAccessControl.VerilyGroup("id1", "cmssynpuf", "name1@verilygroups.com"),
        List.of(USER_1.getEmail(), USER_2.getEmail()));
    vgImpl.addGroup(
        new VerilyGroupsAccessControl.VerilyGroup("id2", "aousynthetic", "name2@verilygroups.com"),
        List.of(USER_2.getEmail()));
    vgImpl.addGroup(
        new VerilyGroupsAccessControl.VerilyGroup("id3", "all", "name3@verilygroups.com"),
        List.of(USER_4.getEmail()));

    impl = vgImpl;
    impl.initialize(
        List.of(CMS_SYNPUF, "cmssynpuf", AOU_SYNTHETIC, "aousynthetic", ALL_ACCESS, "all"),
        "FAKE_BASE_PATH",
        "FAKE_OAUTH_CLIENT_ID");
  }

  @AfterEach
  protected void cleanup() {
    deleteStudies();
  }

  @Test
  void underlay() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId cmsSynpufId = ResourceId.forUnderlay(CMS_SYNPUF);
    ResourceId aouSyntheticId = ResourceId.forUnderlay(AOU_SYNTHETIC);
    ResourceId sddId = ResourceId.forUnderlay(SDD);
    assertHasPermissions(USER_1, cmsSynpufId);
    assertDoesNotHavePermissions(USER_1, aouSyntheticId);
    assertDoesNotHavePermissions(USER_1, sddId);
    assertHasPermissions(USER_2, cmsSynpufId);
    assertHasPermissions(USER_2, aouSyntheticId);
    assertDoesNotHavePermissions(USER_2, sddId);
    assertDoesNotHavePermissions(USER_3, cmsSynpufId);
    assertDoesNotHavePermissions(USER_3, aouSyntheticId);
    assertDoesNotHavePermissions(USER_3, sddId);
    assertHasPermissions(USER_4, cmsSynpufId);
    assertHasPermissions(USER_4, aouSyntheticId);
    assertHasPermissions(USER_4, sddId);

    // service.list
    assertServiceListWithReadPermission(USER_1, ResourceType.UNDERLAY, null, false, cmsSynpufId);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.UNDERLAY, null, false, cmsSynpufId, aouSyntheticId);
    assertServiceListWithReadPermission(USER_3, ResourceType.UNDERLAY, null, false);
    assertServiceListWithReadPermission(
        USER_4, ResourceType.UNDERLAY, null, true, cmsSynpufId, aouSyntheticId, sddId);
  }

  @Test
  void study() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    Action[] studyActionsExceptCreate = {
      Action.READ, Action.UPDATE, Action.DELETE, Action.CREATE_COHORT, Action.CREATE_CONCEPT_SET,
    };
    ResourceId study1Id = ResourceId.forStudy(study1.getId());
    ResourceId study2Id = ResourceId.forStudy(study2.getId());
    assertHasPermissions(USER_1, study1Id, studyActionsExceptCreate);
    assertHasPermissions(USER_1, study2Id, studyActionsExceptCreate);
    assertHasPermissions(USER_2, study1Id, studyActionsExceptCreate);
    assertHasPermissions(USER_2, study2Id, studyActionsExceptCreate);
    assertHasPermissions(USER_3, study1Id, studyActionsExceptCreate);
    assertHasPermissions(USER_3, study2Id, studyActionsExceptCreate);
    assertHasPermissions(USER_4, study1Id, studyActionsExceptCreate);
    assertHasPermissions(USER_4, study2Id, studyActionsExceptCreate);

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
    assertServiceListWithReadPermission(USER_1, ResourceType.STUDY, null, true, study1Id, study2Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.STUDY, null, true, study1Id, study2Id);
    assertServiceListWithReadPermission(USER_3, ResourceType.STUDY, null, true, study1Id, study2Id);
    assertServiceListWithReadPermission(USER_4, ResourceType.STUDY, null, true, study1Id, study2Id);
  }

  @Test
  void cohort() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId cohort1Id = ResourceId.forCohort(study1.getId(), cohort1.getId());
    ResourceId cohort2Id = ResourceId.forCohort(study2.getId(), cohort2.getId());
    assertHasPermissions(USER_1, cohort1Id);
    assertHasPermissions(USER_1, cohort2Id);
    assertHasPermissions(USER_2, cohort1Id);
    assertHasPermissions(USER_2, cohort2Id);
    assertHasPermissions(USER_3, cohort1Id);
    assertHasPermissions(USER_3, cohort2Id);
    assertHasPermissions(USER_4, cohort1Id);
    assertHasPermissions(USER_4, cohort2Id);

    // service.list
    ResourceId study1Id = cohort1Id.getParent();
    ResourceId study2Id = cohort2Id.getParent();
    assertServiceListWithReadPermission(USER_1, ResourceType.COHORT, study1Id, true, cohort1Id);
    assertServiceListWithReadPermission(USER_1, ResourceType.COHORT, study2Id, true, cohort2Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.COHORT, study1Id, true, cohort1Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.COHORT, study2Id, true, cohort2Id);
    assertServiceListWithReadPermission(USER_3, ResourceType.COHORT, study1Id, true, cohort1Id);
    assertServiceListWithReadPermission(USER_3, ResourceType.COHORT, study2Id, true, cohort2Id);
    assertServiceListWithReadPermission(USER_4, ResourceType.COHORT, study1Id, true, cohort1Id);
    assertServiceListWithReadPermission(USER_4, ResourceType.COHORT, study2Id, true, cohort2Id);
  }

  @Test
  void conceptSet() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId conceptSet1Id = ResourceId.forConceptSet(study1.getId(), conceptSet1.getId());
    ResourceId conceptSet2Id = ResourceId.forConceptSet(study2.getId(), conceptSet2.getId());
    assertHasPermissions(USER_1, conceptSet1Id);
    assertHasPermissions(USER_1, conceptSet2Id);
    assertHasPermissions(USER_2, conceptSet1Id);
    assertHasPermissions(USER_2, conceptSet2Id);
    assertHasPermissions(USER_3, conceptSet1Id);
    assertHasPermissions(USER_3, conceptSet2Id);
    assertHasPermissions(USER_4, conceptSet1Id);
    assertHasPermissions(USER_4, conceptSet2Id);

    // service.list
    ResourceId study1Id = conceptSet1Id.getParent();
    ResourceId study2Id = conceptSet2Id.getParent();
    assertServiceListWithReadPermission(
        USER_1, ResourceType.CONCEPT_SET, study1Id, true, conceptSet1Id);
    assertServiceListWithReadPermission(
        USER_1, ResourceType.CONCEPT_SET, study2Id, true, conceptSet2Id);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.CONCEPT_SET, study1Id, true, conceptSet1Id);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.CONCEPT_SET, study2Id, true, conceptSet2Id);
    assertServiceListWithReadPermission(
        USER_3, ResourceType.CONCEPT_SET, study1Id, true, conceptSet1Id);
    assertServiceListWithReadPermission(
        USER_3, ResourceType.CONCEPT_SET, study2Id, true, conceptSet2Id);
    assertServiceListWithReadPermission(
        USER_4, ResourceType.CONCEPT_SET, study1Id, true, conceptSet1Id);
    assertServiceListWithReadPermission(
        USER_4, ResourceType.CONCEPT_SET, study2Id, true, conceptSet2Id);
  }

  @Test
  void review() {
    // isAuthorized, getPermissions, listAllPermissions, listAuthorizedResources
    ResourceId review1Id = ResourceId.forReview(study1.getId(), cohort1.getId(), review1.getId());
    ResourceId review2Id = ResourceId.forReview(study2.getId(), cohort2.getId(), review2.getId());
    assertHasPermissions(USER_1, review1Id);
    assertHasPermissions(USER_1, review2Id);
    assertHasPermissions(USER_2, review1Id);
    assertHasPermissions(USER_2, review2Id);
    assertHasPermissions(USER_3, review1Id);
    assertHasPermissions(USER_3, review2Id);
    assertHasPermissions(USER_4, review1Id);
    assertHasPermissions(USER_4, review2Id);

    // service.list
    ResourceId cohort1Id = review1Id.getParent();
    ResourceId cohort2Id = review2Id.getParent();
    assertServiceListWithReadPermission(USER_1, ResourceType.REVIEW, cohort1Id, true, review1Id);
    assertServiceListWithReadPermission(USER_1, ResourceType.REVIEW, cohort2Id, true, review2Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.REVIEW, cohort1Id, true, review1Id);
    assertServiceListWithReadPermission(USER_2, ResourceType.REVIEW, cohort2Id, true, review2Id);
    assertServiceListWithReadPermission(USER_3, ResourceType.REVIEW, cohort1Id, true, review1Id);
    assertServiceListWithReadPermission(USER_3, ResourceType.REVIEW, cohort2Id, true, review2Id);
    assertServiceListWithReadPermission(USER_4, ResourceType.REVIEW, cohort1Id, true, review1Id);
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
    assertHasPermissions(USER_1, annotationKey2Id);
    assertHasPermissions(USER_2, annotationKey1Id);
    assertHasPermissions(USER_2, annotationKey2Id);
    assertHasPermissions(USER_3, annotationKey1Id);
    assertHasPermissions(USER_3, annotationKey2Id);
    assertHasPermissions(USER_4, annotationKey1Id);
    assertHasPermissions(USER_4, annotationKey2Id);

    // service.list
    ResourceId cohort1Id = annotationKey1Id.getParent();
    ResourceId cohort2Id = annotationKey2Id.getParent();
    assertServiceListWithReadPermission(
        USER_1, ResourceType.ANNOTATION_KEY, cohort1Id, true, annotationKey1Id);
    assertServiceListWithReadPermission(
        USER_1, ResourceType.ANNOTATION_KEY, cohort2Id, true, annotationKey2Id);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.ANNOTATION_KEY, cohort1Id, true, annotationKey1Id);
    assertServiceListWithReadPermission(
        USER_2, ResourceType.ANNOTATION_KEY, cohort2Id, true, annotationKey2Id);
    assertServiceListWithReadPermission(
        USER_3, ResourceType.ANNOTATION_KEY, cohort1Id, true, annotationKey1Id);
    assertServiceListWithReadPermission(
        USER_3, ResourceType.ANNOTATION_KEY, cohort2Id, true, annotationKey2Id);
    assertServiceListWithReadPermission(
        USER_4, ResourceType.ANNOTATION_KEY, cohort1Id, true, annotationKey1Id);
    assertServiceListWithReadPermission(
        USER_4, ResourceType.ANNOTATION_KEY, cohort2Id, true, annotationKey2Id);
  }
}
