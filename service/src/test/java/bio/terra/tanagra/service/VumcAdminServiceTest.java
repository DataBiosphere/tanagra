package bio.terra.tanagra.service;

import bio.terra.tanagra.app.Main;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Disabled("VUMC admin service OAuth client id is not checked into this repo. You can run this test locally by setting the vumc-admin properties in application-test.yaml.")
public class VumcAdminServiceTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(VumcAdminServiceTest.class);

  @Autowired private VumcAdminService vumcAdminService;

  @Test
  void version() throws ApiException {
    SystemVersion systemVersion = vumcAdminService.version();
    LOGGER.info("version returned success: {}", systemVersion);
  }

  @Test
  void roundTrip() throws ApiException {
    CoreServiceTest coreServiceTest = vumcAdminService.roundTripTest();
    LOGGER.info("round trip core -> admin -> core: {}", coreServiceTest);
  }
}
