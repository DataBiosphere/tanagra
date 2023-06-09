package bio.terra.tanagra.service.export;

import java.util.Collections;
import java.util.Map;

public final class ExportResult {
  public enum Status {
    COMPLETE,
    RUNNING
  }

  private final Map<String, String> outputs;
  private final String redirectAwayUrl;
  private final Status status;

  private ExportResult(Map<String, String> outputs, String redirectAwayUrl, Status status) {
    this.outputs = outputs;
    this.redirectAwayUrl = redirectAwayUrl;
    this.status = status;
  }

  public static ExportResult forOutputParams(Map<String, String> outputs, Status status) {
    return new ExportResult(outputs, null, status);
  }

  public static ExportResult forRedirectUrl(String redirectAwayUrl, Status status) {
    return new ExportResult(Collections.emptyMap(), redirectAwayUrl, status);
  }

  public Map<String, String> getOutputs() {
    return Collections.unmodifiableMap(outputs);
  }

  public String getRedirectAwayUrl() {
    return redirectAwayUrl;
  }

  public Status getStatus() {
    return status;
  }
}
