package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.TestApi;
import bio.terra.tanagra.generated.model.ApiVumcAdminServiceTest;
import bio.terra.tanagra.service.VumcAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

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
            version = vumcAdminService.version().toString();
        } catch (Exception ex) {
            LOGGER.error("version", ex);
            version = "version=error: " + ex.getMessage();
        }

        String roundTrip;
        try {
            roundTrip = vumcAdminService.roundtripTest().toString();
        } catch (Exception ex) {
            LOGGER.error("roundTrip", ex);
            roundTrip = "roundTrip=error: " + ex.getMessage();
        }

        return ResponseEntity.ok(new ApiVumcAdminServiceTest().version(version).roundTrip(roundTrip));
    }
}
