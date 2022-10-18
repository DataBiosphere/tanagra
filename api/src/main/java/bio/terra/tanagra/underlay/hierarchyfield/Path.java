package bio.terra.tanagra.underlay.hierarchyfield;

import static bio.terra.tanagra.underlay.HierarchyMapping.PATH_FIELD_NAME;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.HierarchyMapping;
import java.util.List;

public class Path extends HierarchyField {
  @Override
  public Type getType() {
    return Type.PATH;
  }

  @Override
  public String getHierarchyFieldAlias() {
    return getColumnNamePrefix() + PATH_FIELD_NAME;
  }

  @Override
  public ColumnSchema buildColumnSchema() {
    return new ColumnSchema(getHierarchyFieldAlias(), CellValue.SQLDataType.STRING);
  }

  @Override
  public FieldVariable buildFieldVariableFromEntityId(
      HierarchyMapping hierarchyMapping,
      TableVariable entityTableVar,
      List<TableVariable> tableVars) {
    return hierarchyMapping
        .buildPathNumChildrenFieldPointerFromEntityId(PATH_FIELD_NAME)
        .buildVariable(entityTableVar, tableVars, getHierarchyFieldAlias());
  }
}
