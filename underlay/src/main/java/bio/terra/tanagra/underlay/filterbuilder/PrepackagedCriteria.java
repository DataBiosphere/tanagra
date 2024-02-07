package bio.terra.tanagra.underlay.filterbuilder;

import bio.terra.tanagra.filterbuilder.SelectionData;
import java.util.List;

public class PrepackagedCriteria {
  private final String name;
  private final String criteriaSelectorName;
  private final List<SelectionData> selectionData;

  public PrepackagedCriteria(
      String name, String criteriaSelectorName, List<SelectionData> selectionData) {
    this.name = name;
    this.criteriaSelectorName = criteriaSelectorName;
    this.selectionData = selectionData;
  }

  public String getName() {
    return name;
  }

  public String getCriteriaSelectorName() {
    return criteriaSelectorName;
  }

  public List<SelectionData> getSelectionData() {
    return selectionData;
  }
}
