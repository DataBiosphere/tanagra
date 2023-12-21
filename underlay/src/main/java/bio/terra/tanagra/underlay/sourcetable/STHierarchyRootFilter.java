package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlTable;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;

public class STHierarchyRootFilter extends SourceTable {
  private final String entity;
  private final String hierarchy;
  private final SqlColumnSchema idColumnSchema;

  public STHierarchyRootFilter(SqlTable sqlTable, String entity, SZEntity.Hierarchy szHierarchy) {
    super(sqlTable);
    this.entity = entity;
    this.hierarchy = szHierarchy.name;
    this.idColumnSchema = new SqlColumnSchema(szHierarchy.rootIdFieldName, DataType.INT64);
  }

  @Override
  public ImmutableList<SqlColumnSchema> getColumnSchemas() {
    return ImmutableList.of(idColumnSchema);
  }

  public String getEntity() {
    return entity;
  }

  public String getHierarchy() {
    return hierarchy;
  }

  public SqlField getIdField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(idColumnSchema.getColumnName())
        .build();
  }

  public SqlColumnSchema getIdColumnSchema() {
    return idColumnSchema;
  }
}
