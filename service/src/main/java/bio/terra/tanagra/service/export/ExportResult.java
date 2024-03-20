package bio.terra.tanagra.service.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class ExportResult {
  public enum Status {
    COMPLETE,
    FAILED
  }

  private final ImmutableMap<String, String> outputs;
  private final @Nullable String redirectAwayUrl;
  private final Status status;
  private final @Nullable ExportError error;
  private final ImmutableList<ExportFileResult> fileResults;

  private ExportResult(
      @Nullable Map<String, String> outputs,
      @Nullable String redirectAwayUrl,
      Status status,
      @Nullable ExportError error,
      @Nullable List<ExportFileResult> fileResults) {
    this.outputs = outputs == null ? ImmutableMap.of() : ImmutableMap.copyOf(outputs);
    this.redirectAwayUrl = redirectAwayUrl;
    this.status = status;
    this.error = error;
    this.fileResults = fileResults == null ? ImmutableList.of() : ImmutableList.copyOf(fileResults);
  }

  public static ExportResult forOutputParams(
      Map<String, String> outputs, List<ExportFileResult> fileResults) {
    return new ExportResult(outputs, null, Status.COMPLETE, null, fileResults);
  }

  public static ExportResult forRedirectUrl(
      String redirectAwayUrl, List<ExportFileResult> fileResults) {
    return new ExportResult(Map.of(), redirectAwayUrl, Status.COMPLETE, null, fileResults);
  }

  public static ExportResult forError(ExportError error) {
    return new ExportResult(Map.of(), null, Status.FAILED, error, null);
  }

  public ImmutableMap<String, String> getOutputs() {
    return outputs;
  }

  public @Nullable String getRedirectAwayUrl() {
    return redirectAwayUrl;
  }

  public Status getStatus() {
    return status;
  }

  public boolean isSuccessful() {
    return error == null
        && fileResults.stream()
            .filter(fileResult -> !fileResult.isSuccessful())
            .collect(Collectors.toList())
            .isEmpty();
  }

  public @Nullable ExportError getError() {
    return error;
  }

  public ImmutableList<ExportFileResult> getFileResults() {
    return fileResults;
  }
}
