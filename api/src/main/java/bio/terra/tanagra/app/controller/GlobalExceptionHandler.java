package bio.terra.tanagra.app.controller;

import bio.terra.common.exception.AbstractGlobalExceptionHandler;
import bio.terra.tanagra.generated.model.ApiErrorReportV2;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Top-level exception handler for controllers.
 *
 * <p>All exceptions that rise through the controllers are caught in this handler. It converts the
 * exceptions into standard {@link ApiErrorReportV2} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler<ApiErrorReportV2> {

  @Override
  public ApiErrorReportV2 generateErrorReport(
      Throwable ex, HttpStatus statusCode, List<String> causes) {
    return new ApiErrorReportV2()
        .message(ex.getMessage())
        .statusCode(statusCode.value())
        .causes(causes);
  }
}
