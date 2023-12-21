package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlTable;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;

public class STHierarchyChildParent extends SourceTable {
  private final String entity;
  private final String hierarchy;
  private final ColumnSchema childColumnSchema;
  private final ColumnSchema parentColumnSchema;

  public STHierarchyChildParent(SqlTable sqlTable, String entity, SZEntity.Hierarchy szHierarchy) {
    super(sqlTable);
    this.entity = entity;
    this.hierarchy = szHierarchy.name;
    this.childColumnSchema = new ColumnSchema(szHierarchy.childIdFieldName, DataType.INT64);
    this.parentColumnSchema = new ColumnSchema(szHierarchy.parentIdFieldName, DataType.INT64);
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

  public SqlField getChildField() {
    return SqlField.of(getTablePointer(), childColumnSchema.getColumnName());
  }

  public SqlField getParentField() {
    return SqlField.of(getTablePointer(), parentColumnSchema.getColumnName());
  }

  public ColumnSchema getChildColumnSchema() {
    return childColumnSchema;
  }

  public ColumnSchema getParentColumnSchema() {
    return parentColumnSchema;
  }
}
