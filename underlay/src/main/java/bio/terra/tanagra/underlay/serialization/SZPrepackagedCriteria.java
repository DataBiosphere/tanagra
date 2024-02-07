package bio.terra.tanagra.underlay.serialization;

import java.util.List;

public class SZPrepackagedCriteria {
  public String name;
  public String title;
  public String criteriaSelectorName;
  public List<SelectionData> selectionData;

  public static class SelectionData {
    public String pluginName;
    public String serializedFile;
  }
}
