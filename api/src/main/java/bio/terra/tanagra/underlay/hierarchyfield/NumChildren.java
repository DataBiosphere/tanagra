package bio.terra.tanagra.underlay.hierarchyfield;

import static bio.terra.tanagra.underlay.HierarchyMapping.NUM_CHILDREN_FIELD_NAME;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.HierarchyMapping;
import java.util.List;

public class NumChildren extends HierarchyField {
  @Override
  public Type getType() {
    return Type.NUM_CHILDREN;
  }

  @Override
  public String getHierarchyFieldAlias() {
    return getColumnNamePrefix() + NUM_CHILDREN_FIELD_NAME;
  }

  @Override
  public ColumnSchema buildColumnSchema() {
    return new ColumnSchema(getHierarchyFieldAlias(), CellValue.SQLDataType.INT64);
  }

  @Override
  public FieldVariable buildFieldVariableFromEntityId(
      HierarchyMapping hierarchyMapping,
      TableVariable entityTableVar,
      List<TableVariable> tableVars) {
    return hierarchyMapping
        .buildPathNumChildrenFieldPointerFromEntityId(NUM_CHILDREN_FIELD_NAME)
        .buildVariable(entityTableVar, tableVars, getHierarchyFieldAlias());
  }
}
