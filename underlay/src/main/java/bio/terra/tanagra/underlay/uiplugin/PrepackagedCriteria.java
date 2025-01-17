package bio.terra.tanagra.underlay.uiplugin;

import org.apache.commons.lang3.StringUtils;

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
    return StringUtils.isNotEmpty(pluginData);
  }

  public SelectionData getSelectionData() {
    return hasSelectionData() ? new SelectionData(null, pluginData) : null;
  }
}
