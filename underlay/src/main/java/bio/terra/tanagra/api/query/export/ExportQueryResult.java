package bio.terra.tanagra.api.query.export;

public class ExportQueryResult {
  private final String fileName;

  public ExportQueryResult(String fileName) {
    this.fileName = fileName;
  }

  public String getFileName() {
    return fileName;
  }
}
