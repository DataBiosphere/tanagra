package bio.terra.tanagra.api.query.export;

import bio.terra.tanagra.api.query.list.ListQueryRequest;

public class ExportQueryRequest {
  private final ListQueryRequest listQueryRequest;
  private final String fileContents;
  private final String fileDisplayName;
  private final String fileNamePrefix;
  private final boolean generateUnsignedUrl;

  private ExportQueryRequest(
      ListQueryRequest listQueryRequest,
      String fileContents,
      String fileDisplayName,
      String fileNamePrefix,
      boolean generateUnsignedUrl) {
    this.listQueryRequest = listQueryRequest;
    this.fileContents = fileContents;
    this.fileDisplayName = fileDisplayName;
    this.fileNamePrefix = fileNamePrefix;
    this.generateUnsignedUrl = generateUnsignedUrl;
  }

  public static ExportQueryRequest forListQuery(
      ListQueryRequest listQueryRequest,
      String fileDisplayName,
      String fileNamePrefix,
      boolean generateUnsignedUrl) {
    return new ExportQueryRequest(
        listQueryRequest, null, fileDisplayName, fileNamePrefix, generateUnsignedUrl);
  }

  public static ExportQueryRequest forRawData(
      String fileContents, String fileDisplayName, boolean generateUnsignedUrl) {
    return new ExportQueryRequest(null, fileContents, fileDisplayName, null, generateUnsignedUrl);
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

  public boolean isGenerateUnsignedUrl() {
    return generateUnsignedUrl;
  }
}
