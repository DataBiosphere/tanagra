package bio.terra.tanagra.underlay.uiplugin;

public class PrepackagedCriteria {
  private final String name;
  private final String criteriaSelector;
  private final String pluginData;

  public PrepackagedCriteria(String name, String criteriaSelector, String pluginData) {
    this.name = name;
    this.criteriaSelector = criteriaSelector;
    this.pluginData = pluginData;
  }

  public String getName() {
    return name;
  }

  public String getCriteriaSelector() {
    return criteriaSelector;
  }

  public boolean hasSelectionData() {
    return pluginData != null && !pluginData.isEmpty();
  }

  public SelectionData getSelectionData() {
    return hasSelectionData() ? new SelectionData(null, pluginData) : null;
  }
}
