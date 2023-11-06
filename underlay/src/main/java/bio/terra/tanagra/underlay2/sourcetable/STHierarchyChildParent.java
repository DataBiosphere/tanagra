package bio.terra.tanagra.underlay2.sourcetable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay2.serialization.SZEntity;
import com.google.common.collect.ImmutableList;

public class STHierarchyChildParent extends SourceTable {
  private final String entity;
  private final String hierarchy;
  private final ColumnSchema childColumnSchema;
  private final ColumnSchema parentColumnSchema;

  public STHierarchyChildParent(
      TablePointer tablePointer, String entity, SZEntity.Hierarchy szHierarchy) {
    super(tablePointer);
    this.entity = entity;
    this.hierarchy = szHierarchy.name;
    this.childColumnSchema =
        new ColumnSchema(szHierarchy.childIdFieldName, CellValue.SQLDataType.INT64);
    this.parentColumnSchema =
        new ColumnSchema(szHierarchy.parentIdFieldName, CellValue.SQLDataType.INT64);
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return ImmutableList.of(childColumnSchema, parentColumnSchema);
  }

  public String getEntity() {
    return entity;
  }

  public String getHierarchy() {
    return hierarchy;
  }

  public FieldPointer getChildField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(childColumnSchema.getColumnName())
        .build();
  }

  public FieldPointer getParentField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(parentColumnSchema.getColumnName())
        .build();
  }

  public ColumnSchema getChildColumnSchema() {
    return childColumnSchema;
  }

  public ColumnSchema getParentColumnSchema() {
    return parentColumnSchema;
  }
}
