package bio.terra.tanagra.underlay;

import static bio.terra.tanagra.query.Query.TANAGRA_FIELD_PREFIX;

public class HierarchyField {
  public enum FieldName {
    PATH,
    NUM_CHILDREN,
    IS_ROOT
  }

  private final String hierarchyName;
  private final FieldName fieldName;

  public HierarchyField(String hierarchyName, FieldName fieldName) {
    this.hierarchyName = hierarchyName;
    this.fieldName = fieldName;
  }

  public String getColumnNamePrefix() {
    return TANAGRA_FIELD_PREFIX + hierarchyName + "_";
  }

  public String getHierarchyName() {
    return hierarchyName;
  }

  public FieldName getFieldName() {
    return fieldName;
  }
}
