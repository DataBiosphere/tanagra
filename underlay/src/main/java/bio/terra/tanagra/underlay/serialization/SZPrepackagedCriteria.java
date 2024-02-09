package bio.terra.tanagra.underlay.serialization;

import java.util.List;

public class SZPrepackagedCriteria {
  public String name;
  public String displayName;
  public String criteriaSelector;
  public List<SelectionData> selectionData;

  public static class SelectionData {
    public String plugin;
    public String pluginData;
    public String pluginDataFile;
  }
}
