package bio.terra.tanagra.service.export;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class ExportResult {
  private final Map<String, String> outputs;
  private final @Nullable String redirectAwayUrl;
  private final @Nullable ExportError error;
  private final ImmutableList<ExportFileResult> fileResults;

  private ExportResult(
      @Nullable Map<String, String> outputs,
      @Nullable String redirectAwayUrl,
      @Nullable ExportError error,
      @Nullable List<ExportFileResult> fileResults) {
    this.outputs = outputs == null ? Map.of() : outputs;
    this.redirectAwayUrl = redirectAwayUrl;
    this.error = error;
    this.fileResults = fileResults == null ? ImmutableList.of() : ImmutableList.copyOf(fileResults);
  }

  public static ExportResult forOutputParams(
      Map<String, String> outputs, List<ExportFileResult> fileResults) {
    return new ExportResult(outputs, null, null, fileResults);
  }

  public static ExportResult forRedirectUrl(
      String redirectAwayUrl, List<ExportFileResult> fileResults) {
    return new ExportResult(Map.of(), redirectAwayUrl, null, fileResults);
  }

  public static ExportResult forError(ExportError error) {
    return new ExportResult(Map.of(), null, error, null);
  }

  public Map<String, String> getOutputs() {
    return Collections.unmodifiableMap(outputs);
  }

  public @Nullable String getRedirectAwayUrl() {
    return redirectAwayUrl;
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
