package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.Permissions;
import bio.terra.tanagra.service.accesscontrol.ResourceCollection;
import bio.terra.tanagra.service.accesscontrol.ResourceId;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol.impl.AouWorkbenchAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.VerilyGroupsAccessControl;
import bio.terra.tanagra.service.accesscontrol.impl.VumcAdminAccessControl;
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

  @Disabled(
      "VUMC admin service base path + oauth client id are not checked into this repo. You can run this test locally by setting the access-control properties in application-test.yaml.")
  @Test
  void vumcAdmin() throws ApiException {
    VumcAdminAccessControl impl = new VumcAdminAccessControl();
    impl.initialize(
        accessControlConfiguration.getParams(),
        accessControlConfiguration.getBasePath(),
        accessControlConfiguration.getOauthClientId());

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
        impl.isAuthorized(
            UserId.forDisabledAuthentication(),
            Permissions.forActions(ResourceType.UNDERLAY, Action.READ),
            firstUnderlay));
    assertFalse(
        impl.listAuthorizedResources(
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
    impl.initialize(
        accessControlConfiguration.getParams(),
        accessControlConfiguration.getBasePath(),
        accessControlConfiguration.getOauthClientId());

    // Access control is only on underlays, no other resource types.
    assertTrue(
        impl.isAuthorized(
            UserId.forDisabledAuthentication(),
            Permissions.forActions(ResourceType.STUDY, Action.CREATE),
            null));
    assertTrue(
        impl.listAuthorizedResources(
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
    impl.initialize(
        accessControlConfiguration.getParams(),
        accessControlConfiguration.getBasePath(),
        accessControlConfiguration.getOauthClientId());

    // Access control is only on studies, no other resource types.
    ResourceId firstUnderlay =
        ResourceId.forUnderlay(
            underlayService
                .listUnderlays(
                    ResourceCollection.allResourcesAllPermissions(ResourceType.UNDERLAY, null))
                .get(0)
                .getName());
    assertTrue(
        impl.isAuthorized(
            UserId.forDisabledAuthentication(),
            Permissions.forActions(ResourceType.UNDERLAY, Action.READ),
            firstUnderlay));
    assertFalse(
        impl.listAuthorizedResources(
                UserId.forDisabledAuthentication(),
                Permissions.forActions(ResourceType.UNDERLAY, Action.READ),
                0,
                10)
            .getResources()
            .isEmpty());
  }
}
