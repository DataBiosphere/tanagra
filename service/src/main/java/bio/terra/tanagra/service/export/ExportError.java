package bio.terra.tanagra.service.export;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ExportError {
  private final String message;
  private final String stackTrace;
  private final boolean isTimeout;

  private ExportError(String message, String stackTrace, boolean isTimeout) {
    this.message = message;
    this.stackTrace = stackTrace;
    this.isTimeout = isTimeout;
  }

  public static ExportError forMessage(String message, boolean isTimeout) {
    return new ExportError(message, null, isTimeout);
  }

  public static ExportError forException(Exception ex) {
    StringWriter stackTraceStr = new StringWriter();
    ex.printStackTrace(new PrintWriter(stackTraceStr));
    return new ExportError(ex.getMessage(), stackTraceStr.toString(), false);
  }

  public static ExportError forException(String message, String stackTrace, boolean isTimeout) {
    return new ExportError(message, stackTrace, isTimeout);
  }

  public String getMessage() {
    return message;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public boolean isTimeout() {
    return isTimeout;
  }
}
