package bio.terra.tanagra.api.query.export;

import bio.terra.tanagra.api.query.list.ListQueryRequest;
import java.util.List;

public class ExportQueryRequest {
  private final ListQueryRequest listQueryRequest;
  private final String fileDisplayName;
  private final String fileNamePrefix;
  private final String gcsProjectId;
  private final List<String> availableBqDatasetIds;
  private final List<String> availableGcsBucketNames;
  private final boolean generateSignedUrl;

  public ExportQueryRequest(
      ListQueryRequest listQueryRequest,
      String fileDisplayName,
      String fileNamePrefix,
      String gcsProjectId,
      List<String> availableBqDatasetIds,
      List<String> availableGcsBucketNames,
      boolean generateSignedUrl) {
    this.listQueryRequest = listQueryRequest;
    this.fileDisplayName = fileDisplayName;
    this.fileNamePrefix = fileNamePrefix;
    this.gcsProjectId = gcsProjectId;
    this.availableBqDatasetIds = availableBqDatasetIds;
    this.availableGcsBucketNames = availableGcsBucketNames;
    this.generateSignedUrl = generateSignedUrl;
  }

  public ListQueryRequest getListQueryRequest() {
    return listQueryRequest;
  }

  public String getFileDisplayName() {
    return fileDisplayName;
  }

  public String getFileNamePrefix() {
    return fileNamePrefix;
  }

  public String getGcsProjectId() {
    return gcsProjectId;
  }

  public List<String> getAvailableBqDatasetIds() {
    return availableBqDatasetIds;
  }

  public List<String> getAvailableGcsBucketNames() {
    return availableGcsBucketNames;
  }

  public boolean isGenerateSignedUrl() {
    return generateSignedUrl;
  }
}
