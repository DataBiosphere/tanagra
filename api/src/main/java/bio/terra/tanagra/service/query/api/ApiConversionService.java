package bio.terra.tanagra.service.query.api;

import bio.terra.tanagra.service.underlay.UnderlayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A service to handle validating and converting between OpenAPI classes an Tanagra's internal
 * representation.
 */
@Component
public class ApiConversionService {
  private final UnderlayService underlayService;

  @Autowired
  public ApiConversionService(UnderlayService underlayService) {
    this.underlayService = underlayService;
  }
}
