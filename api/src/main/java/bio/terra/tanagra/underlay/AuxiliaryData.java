package bio.terra.tanagra.underlay;

import java.util.List;

public class AuxiliaryData {
  private String name;
  private List<String> fields;

  public AuxiliaryData(String name, List<String> fields) {
    this.name = name;
    this.fields = fields;
  }

  public String getName() {
    return name;
  }

  public List<String> getFields() {
    return fields;
  }
}
