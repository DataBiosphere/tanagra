package bio.terra.tanagra.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.tanagra.app.controller.UnauthenticatedApiController;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class UnauthenticatedApiControllerTest extends BaseSpringUnitTest {
  @Autowired UnauthenticatedApiController controller;

  @Test
  void systemStatusOk() {
    assertEquals(HttpStatus.OK, controller.serviceStatus().getStatusCode());
  }
}
