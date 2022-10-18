package bio.terra.tanagra.underlay.hierarchyfield;

import static bio.terra.tanagra.underlay.HierarchyMapping.IS_ROOT_FIELD_NAME;
import static bio.terra.tanagra.underlay.HierarchyMapping.PATH_FIELD_NAME;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.HierarchyField;
import bio.terra.tanagra.underlay.HierarchyMapping;
import java.util.List;

public class IsRoot extends HierarchyField {
  public IsRoot(String hierarchyName) {
    super(hierarchyName);
  }

  @Override
  public Type getType() {
    return Type.IS_ROOT;
  }

  @Override
  public String getHierarchyFieldAlias() {
    return getColumnNamePrefix() + IS_ROOT_FIELD_NAME;
  }

  @Override
  public ColumnSchema buildColumnSchema() {
    return new ColumnSchema(getHierarchyFieldAlias(), CellValue.SQLDataType.BOOLEAN);
  }

  @Override
  public FieldVariable buildFieldVariableFromEntityId(
      HierarchyMapping hierarchyMapping,
      FieldPointer entityIdFieldPointer,
      TableVariable entityTableVar,
      List<TableVariable> tableVars) {
    // Currently, this is a calculated field. IS_ROOT means path IS NOT NULL AND path=''.
    FieldPointer pathFieldPointer =
        hierarchyMapping.buildPathNumChildrenFieldPointerFromEntityId(
            entityIdFieldPointer, PATH_FIELD_NAME);

    // TODO: Handle the case where the path field is in the same table (i.e. not FK'd).
    return new FieldPointer.Builder()
        .tablePointer(pathFieldPointer.getTablePointer())
        .columnName(pathFieldPointer.getColumnName())
        .foreignTablePointer(pathFieldPointer.getForeignTablePointer())
        .foreignKeyColumnName(pathFieldPointer.getForeignKeyColumnName())
        .foreignColumnName(pathFieldPointer.getForeignColumnName())
        .sqlFunctionWrapper("(${fieldSql} IS NOT NULL AND ${fieldSql}='')")
        .build()
        .buildVariable(entityTableVar, tableVars, getHierarchyFieldAlias());
  }
}
