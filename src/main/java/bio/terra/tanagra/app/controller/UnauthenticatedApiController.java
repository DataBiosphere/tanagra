package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.UnauthenticatedApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

/** Spring controller for the unauthenticated API methods. */
@Controller
public class UnauthenticatedApiController implements UnauthenticatedApi {
  @Override
  public ResponseEntity<Void> serviceStatus() {
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
