package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.service.accesscontrol.AccessControlService;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol.model.impl.AouWorkbenchAccessControl;
import bio.terra.tanagra.service.accesscontrol.model.impl.VerilyGroupsAccessControl;
import bio.terra.tanagra.service.accesscontrol.model.impl.VumcAdminAccessControl;
import bio.terra.tanagra.service.artifact.CohortService;
import bio.terra.tanagra.service.artifact.ConceptSetService;
import bio.terra.tanagra.service.artifact.ReviewService;
import bio.terra.tanagra.service.artifact.StudyService;
import bio.terra.tanagra.service.authentication.UserId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.vumc.vda.tanagra.admin.client.ApiException;
import org.vumc.vda.tanagra.admin.model.CoreServiceTest;
import org.vumc.vda.tanagra.admin.model.SystemVersion;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
public class AccessControlImplTest {
  @Autowired private AccessControlConfiguration accessControlConfiguration;
  @Autowired private UnderlayService underlayService;
  @Autowired protected StudyService studyService;
  @Autowired protected CohortService cohortService;
  @Autowired protected ConceptSetService conceptSetService;
  @Autowired protected ReviewService reviewService;

  @Disabled(
      "VUMC admin service base path + oauth client id are not checked into this repo. You can run this test locally by setting the access-control properties in application-test.yaml.")
  @Test
  void vumcAdmin() throws ApiException {
    VumcAdminAccessControl impl = new VumcAdminAccessControl();
    AccessControlService accessControlService =
        new AccessControlService(
            impl,
            accessControlConfiguration,
            studyService,
            cohortService,
            conceptSetService,
            reviewService);

    SystemVersion systemVersion = impl.apiVersion();
    assertNotNull(systemVersion);
    CoreServiceTest coreServiceTest = impl.apiRoundTripTest();
    assertNotNull(coreServiceTest);

    // Access control is only on studies, no other resource types.
    ResourceId firstUnderlay =
        ResourceId.forUnderlay(
            underlayService
                .listUnderlays(
                    ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null))
                .get(0)
                .getName());
    assertTrue(
        accessControlService.isAuthorized(
            UserId.forDisabledAuthentication(),
            Permissions.forActions(ResourceType.UNDERLAY, Action.READ),
            firstUnderlay));
    assertFalse(
        accessControlService
            .listAuthorizedResources(
                UserId.forDisabledAuthentication(),
                Permissions.forActions(ResourceType.UNDERLAY, Action.READ),
                0,
                10)
            .getResources()
            .isEmpty());
  }

  @Disabled(
      "VerilyGroup base path + oauth client id are not checked into this repo. You can run this test locally by setting the access-control properties in application-test.yaml.")
  @Test
  void verilyGroups() {
    VerilyGroupsAccessControl impl = new VerilyGroupsAccessControl();
    AccessControlService accessControlService =
        new AccessControlService(
            impl,
            accessControlConfiguration,
            studyService,
            cohortService,
            conceptSetService,
            reviewService);

    // Access control is only on underlays, no other resource types.
    assertTrue(
        accessControlService.isAuthorized(
            UserId.forDisabledAuthentication(),
            Permissions.forActions(ResourceType.STUDY, Action.CREATE),
            null));
    assertTrue(
        accessControlService
            .listAuthorizedResources(
                UserId.forDisabledAuthentication(),
                Permissions.forActions(ResourceType.STUDY, Action.READ),
                0,
                10)
            .isAllResources());
  }

  @Disabled(
      "AoU Workbench service base path + oauth client id are not checked into this repo. You can run this test locally by setting the access-control properties in application-test.yaml.")
  @Test
  void aouWorkbench() {
    AouWorkbenchAccessControl impl = new AouWorkbenchAccessControl();
    AccessControlService accessControlService =
        new AccessControlService(
            impl,
            accessControlConfiguration,
            studyService,
            cohortService,
            conceptSetService,
            reviewService);

    // Access control is only on studies, no other resource types.
    ResourceId firstUnderlay =
        ResourceId.forUnderlay(
            underlayService
                .listUnderlays(
                    ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null))
                .get(0)
                .getName());
    assertTrue(
        accessControlService.isAuthorized(
            UserId.forDisabledAuthentication(),
            Permissions.forActions(ResourceType.UNDERLAY, Action.READ),
            firstUnderlay));
    assertFalse(
        accessControlService
            .listAuthorizedResources(
                UserId.forDisabledAuthentication(),
                Permissions.forActions(ResourceType.UNDERLAY, Action.READ),
                0,
                10)
            .getResources()
            .isEmpty());
  }
}
