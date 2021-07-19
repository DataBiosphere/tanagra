package bio.terra.tanagra.app.controller;

import bio.terra.tanagra.generated.controller.UnauthenticatedApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
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

  /** Required if using Swagger-CodeGen, but actually we don't need this. */
  @Override
  public Optional<ObjectMapper> getObjectMapper() {
    return Optional.empty();
  }

  /** Required if using Swagger-CodeGen, but actually we don't need this. */
  @Override
  public Optional<HttpServletRequest> getRequest() {
    return Optional.empty();
  }
}
