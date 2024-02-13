package bio.terra.tanagra.underlay.uiplugin;

import java.util.List;

public class PrepackagedCriteria {
  private final String name;
  private final String criteriaSelector;
  private final List<SelectionData> selectionData;

  public PrepackagedCriteria(
      String name, String criteriaSelector, List<SelectionData> selectionData) {
    this.name = name;
    this.criteriaSelector = criteriaSelector;
    this.selectionData = selectionData;
  }

  public String getName() {
    return name;
  }

  public String getCriteriaSelector() {
    return criteriaSelector;
  }

  public List<SelectionData> getSelectionData() {
    return selectionData;
  }
}
