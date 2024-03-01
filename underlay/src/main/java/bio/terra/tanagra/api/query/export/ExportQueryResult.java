package bio.terra.tanagra.api.query.export;

public class ExportQueryResult {
  private final String fileDisplayName;
  private final String filePath;

  public ExportQueryResult(String fileDisplayName, String filePath) {
    this.fileDisplayName = fileDisplayName;
    this.filePath = filePath;
  }

  public String getFileDisplayName() {
    return fileDisplayName;
  }

  public String getFilePath() {
    return filePath;
  }
}
