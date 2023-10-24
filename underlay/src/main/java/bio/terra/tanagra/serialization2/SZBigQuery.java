package bio.terra.tanagra.serialization2;

import java.util.Map;

public class SZBigQuery {
  public SourceData sourceData;
  public IndexData indexData;
  public String queryProjectId;
  public String dataLocation;

  public static class SourceData {
    public String projectId;
    public String datasetId;
    public Map<String, String> sqlSubstitutions;
  }

  public static class IndexData {
    public String projectId;
    public String datasetId;
    public String tablePrefix;
  }
}
