package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlTable;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;

public class STHierarchyChildParent extends SourceTable {
  private final String entity;
  private final String hierarchy;
  private final SqlColumnSchema childColumnSchema;
  private final SqlColumnSchema parentColumnSchema;

  public STHierarchyChildParent(SqlTable sqlTable, String entity, SZEntity.Hierarchy szHierarchy) {
    super(sqlTable);
    this.entity = entity;
    this.hierarchy = szHierarchy.name;
    this.childColumnSchema = new SqlColumnSchema(szHierarchy.childIdFieldName, DataType.INT64);
    this.parentColumnSchema = new SqlColumnSchema(szHierarchy.parentIdFieldName, DataType.INT64);
  }

  @Override
  public ImmutableList<SqlColumnSchema> getColumnSchemas() {
    return ImmutableList.of(childColumnSchema, parentColumnSchema);
  }

  public String getEntity() {
    return entity;
  }

  public String getHierarchy() {
    return hierarchy;
  }

  public SqlField getChildField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(childColumnSchema.getColumnName())
        .build();
  }

  public SqlField getParentField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(parentColumnSchema.getColumnName())
        .build();
  }

  public SqlColumnSchema getChildColumnSchema() {
    return childColumnSchema;
  }

  public SqlColumnSchema getParentColumnSchema() {
    return parentColumnSchema;
  }
}
