package bio.terra.tanagra.service.export;

import java.util.Collections;
import java.util.Map;

public class ExportResult {
  public enum Status {
    COMPLETE,
    RUNNING
  }

  private final Map<String, String> outputs;
  private final Status status;

  public ExportResult(Map<String, String> outputs, Status status) {
    this.outputs = outputs;
    this.status = status;
  }

  public Map<String, String> getOutputs() {
    return Collections.unmodifiableMap(outputs);
  }

  public Status getStatus() {
    return status;
  }
}
