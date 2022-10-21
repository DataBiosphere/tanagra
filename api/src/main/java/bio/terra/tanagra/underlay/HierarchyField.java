package bio.terra.tanagra.underlay;

import static bio.terra.tanagra.query.Query.TANAGRA_FIELD_PREFIX;

import bio.terra.tanagra.query.ColumnSchema;
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

  private Hierarchy hierarchy;

  public void initialize(Hierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }

  public abstract Type getType();

  public abstract String getHierarchyFieldAlias();

  public abstract ColumnSchema buildColumnSchema();

  public abstract FieldVariable buildFieldVariableFromEntityId(
      HierarchyMapping hierarchyMapping,
      TableVariable entityTableVar,
      List<TableVariable> tableVars);

  protected String getColumnNamePrefix() {
    return TANAGRA_FIELD_PREFIX + hierarchy.getName() + "_";
  }

  public Hierarchy getHierarchy() {
    return hierarchy;
  }
}
