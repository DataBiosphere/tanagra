package bio.terra.tanagra.service;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.tanagra.app.Main;
import bio.terra.tanagra.app.configuration.AccessControlConfiguration;
import bio.terra.tanagra.service.accesscontrol.Action;
import bio.terra.tanagra.service.accesscontrol.ResourceType;
import bio.terra.tanagra.service.accesscontrol.impl.VerilyGroupsAccessControl;
import bio.terra.tanagra.service.auth.UserId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@ActiveProfiles("test")
public class AccessControlImplTest {
  @Autowired private AccessControlConfiguration accessControlConfiguration;

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
            UserId.forDisabledAuthentication(), Action.CREATE, ResourceType.COHORT, null));
  }
}
