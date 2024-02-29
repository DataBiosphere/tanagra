package bio.terra.tanagra.api.query.export;

import bio.terra.tanagra.api.query.list.ListQueryRequest;
import java.util.List;

public class ExportQueryRequest {
  private final ListQueryRequest listQueryRequest;
  private final String fileNamePrefix;
  private final String gcsProjectId;
  private final List<String> availableBqDatasetIds;
  private final List<String> availableGcsBucketNames;

  public ExportQueryRequest(
      ListQueryRequest listQueryRequest,
      String fileNamePrefix,
      String gcsProjectId,
      List<String> availableBqDatasetIds,
      List<String> availableGcsBucketNames) {
    this.listQueryRequest = listQueryRequest;
    this.fileNamePrefix = fileNamePrefix;
    this.gcsProjectId = gcsProjectId;
    this.availableBqDatasetIds = availableBqDatasetIds;
    this.availableGcsBucketNames = availableGcsBucketNames;
  }

  public ListQueryRequest getListQueryRequest() {
    return listQueryRequest;
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
}
