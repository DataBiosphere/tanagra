package bio.terra.tanagra.app.controller;

import bio.terra.common.exception.AbstractGlobalExceptionHandler;
import bio.terra.tanagra.generated.model.ApiErrorReport;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Top-level exception handler for controllers.
 *
 * <p>All exceptions that rise through the controllers are caught in this handler. It converts the
 * exceptions into standard {@link ApiErrorReport} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler<ApiErrorReport> {

  @Override
  public ApiErrorReport generateErrorReport(
      Throwable ex, HttpStatus statusCode, List<String> causes) {
    return new ApiErrorReport()
        .message(ex.getMessage())
        .statusCode(statusCode.value())
        .causes(causes);
  }
}
