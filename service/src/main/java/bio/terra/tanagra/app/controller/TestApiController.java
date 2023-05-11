package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.TestApi;
import bio.terra.tanagra.generated.model.ApiVumcAdminServiceTest;
import bio.terra.tanagra.service.VumcAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.vumc.vda.tanagra.admin.model.CoreServiceTest;
import org.vumc.vda.tanagra.admin.model.SystemVersion;

@Controller
public class TestApiController implements TestApi {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestApiController.class);

  private final VumcAdminService vumcAdminService;

  @Autowired
  public TestApiController(VumcAdminService vumcAdminService) {
    this.vumcAdminService = vumcAdminService;
  }

  @Override
  public ResponseEntity<ApiVumcAdminServiceTest> vumcAdminServiceTest() {
    String version;
    try {
      SystemVersion adminVersion = vumcAdminService.version();
      version =
          String.format(
              "gitTag: %s, gitHash: %s, github: %s, build: %s",
              adminVersion.getGitTag(),
              adminVersion.getGitHash(),
              adminVersion.getGithub(),
              adminVersion.getBuild());
    } catch (Exception ex) {
      LOGGER.error("admin service version", ex);
      version = "error: " + ex.getMessage();
    }

    String roundTrip;
    try {
      CoreServiceTest coreServiceTest = vumcAdminService.roundTripTest();
      roundTrip =
          String.format(
              "[version] %s, [authenticated-user] %s",
              coreServiceTest.getVersion(), coreServiceTest.getAuthenticatedUser());
    } catch (Exception ex) {
      LOGGER.error("core service authenticated user", ex);
      roundTrip = "error: " + ex.getMessage();
    }

    return ResponseEntity.ok(new ApiVumcAdminServiceTest().version(version).roundTrip(roundTrip));
  }
}
