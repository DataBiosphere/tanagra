package bio.terra.tanagra.api.query.export;

import bio.terra.tanagra.api.query.list.ListQueryRequest;

public class ExportQueryRequest {
  private final ListQueryRequest listQueryRequest;
  private final String fileContents;
  private final String fileDisplayName;
  private final String fileNamePrefix;
  private final boolean generateSignedUrl;

  private ExportQueryRequest(
      ListQueryRequest listQueryRequest,
      String fileContents,
      String fileDisplayName,
      String fileNamePrefix,
      boolean generateSignedUrl) {
    this.listQueryRequest = listQueryRequest;
    this.fileContents = fileContents;
    this.fileDisplayName = fileDisplayName;
    this.fileNamePrefix = fileNamePrefix;
    this.generateSignedUrl = generateSignedUrl;
  }

  public static ExportQueryRequest forListQuery(
      ListQueryRequest listQueryRequest,
      String fileDisplayName,
      String fileNamePrefix,
      boolean generateSignedUrl) {
    return new ExportQueryRequest(
        listQueryRequest, null, fileDisplayName, fileNamePrefix, generateSignedUrl);
  }

  public static ExportQueryRequest forRawData(
      String fileContents, String fileDisplayName, boolean generateSignedUrl) {
    return new ExportQueryRequest(null, fileContents, fileDisplayName, null, generateSignedUrl);
  }

  public ListQueryRequest getListQueryRequest() {
    return listQueryRequest;
  }

  public String getFileContents() {
    return fileContents;
  }

  public boolean isForRawData() {
    return listQueryRequest == null;
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
