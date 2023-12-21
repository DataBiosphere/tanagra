package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlTable;
import com.google.common.collect.ImmutableList;

public class STRelationshipIdPairs extends SourceTable {
  private final String entityGroup;
  private final String entityA;
  private final String entityB;
  private final SqlColumnSchema entityAIdColumnSchema;
  private final SqlColumnSchema entityBIdColumnSchema;

  public STRelationshipIdPairs(
      SqlTable sqlTable,
      String entityGroup,
      String entityA,
      String entityB,
      String entityAIdFieldName,
      String entityBIdFieldName) {
    super(sqlTable);
    this.entityGroup = entityGroup;
    this.entityA = entityA;
    this.entityB = entityB;
    this.entityAIdColumnSchema = new SqlColumnSchema(entityAIdFieldName, DataType.INT64);
    this.entityBIdColumnSchema = new SqlColumnSchema(entityBIdFieldName, DataType.INT64);
  }

  @Override
  public ImmutableList<SqlColumnSchema> getColumnSchemas() {
    return ImmutableList.of(entityAIdColumnSchema, entityBIdColumnSchema);
  }

  public String getEntityGroup() {
    return entityGroup;
  }

  public String getEntityA() {
    return entityA;
  }

  public String getEntityB() {
    return entityB;
  }

  public SqlField getEntityAIdField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(entityAIdColumnSchema.getColumnName())
        .build();
  }

  public SqlColumnSchema getEntityAIdColumnSchema() {
    return entityAIdColumnSchema;
  }

  public SqlField getEntityBIdField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(entityBIdColumnSchema.getColumnName())
        .build();
  }

  public SqlColumnSchema getEntityBIdColumnSchema() {
    return entityBIdColumnSchema;
  }
}
