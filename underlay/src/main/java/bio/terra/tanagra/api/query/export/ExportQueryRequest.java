package bio.terra.tanagra.api.query.export;

import bio.terra.tanagra.api.query.list.ListQueryRequest;

public class ExportQueryRequest {
  private final ListQueryRequest listQueryRequest;
  private final String fileDisplayName;
  private final String fileNamePrefix;
  private final boolean generateSignedUrl;

  public ExportQueryRequest(
      ListQueryRequest listQueryRequest,
      String fileDisplayName,
      String fileNamePrefix,
      boolean generateSignedUrl) {
    this.listQueryRequest = listQueryRequest;
    this.fileDisplayName = fileDisplayName;
    this.fileNamePrefix = fileNamePrefix;
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

  public boolean isGenerateSignedUrl() {
    return generateSignedUrl;
  }
}
