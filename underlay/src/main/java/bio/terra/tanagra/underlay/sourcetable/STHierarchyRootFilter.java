package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;

public class STHierarchyRootFilter extends SourceTable {
  private final String entity;
  private final String hierarchy;
  private final ColumnSchema idColumnSchema;

  public STHierarchyRootFilter(
      TablePointer tablePointer, String entity, SZEntity.Hierarchy szHierarchy) {
    super(tablePointer);
    this.entity = entity;
    this.hierarchy = szHierarchy.name;
    this.idColumnSchema =
        new ColumnSchema(szHierarchy.rootIdFieldName, CellValue.SQLDataType.INT64);
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return ImmutableList.of(idColumnSchema);
  }

  public String getEntity() {
    return entity;
  }

  public String getHierarchy() {
    return hierarchy;
  }

  public FieldPointer getIdField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(idColumnSchema.getColumnName())
        .build();
  }

  public ColumnSchema getIdColumnSchema() {
    return idColumnSchema;
  }
}
