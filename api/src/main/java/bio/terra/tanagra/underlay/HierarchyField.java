package bio.terra.tanagra.underlay;

import static bio.terra.tanagra.query.Query.TANAGRA_FIELD_PREFIX;

import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.TableVariable;
import java.util.List;

public abstract class HierarchyField {
  public enum Type {
    IS_MEMBER,
    PATH,
    NUM_CHILDREN,
    IS_ROOT
  }

  private final String hierarchyName;

  protected HierarchyField(String hierarchyName) {
    this.hierarchyName = hierarchyName;
  }

  public abstract Type getType();

  public abstract String getHierarchyFieldAlias();

  public abstract ColumnSchema buildColumnSchema();

  public abstract FieldVariable buildFieldVariableFromEntityId(
      HierarchyMapping hierarchyMapping,
      FieldPointer entityIdFieldPointer,
      TableVariable entityTableVar,
      List<TableVariable> tableVars);

  protected String getColumnNamePrefix() {
    return TANAGRA_FIELD_PREFIX + hierarchyName + "_";
  }

  public String getHierarchyName() {
    return hierarchyName;
  }
}
