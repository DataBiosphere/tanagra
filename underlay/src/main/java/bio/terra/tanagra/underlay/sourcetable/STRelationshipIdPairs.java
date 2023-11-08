package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import com.google.common.collect.ImmutableList;

public class STRelationshipIdPairs extends SourceTable {
  private final String entityGroup;
  private final String entityA;
  private final String entityB;
  private final ColumnSchema entityAIdColumnSchema;
  private final ColumnSchema entityBIdColumnSchema;

  public STRelationshipIdPairs(
      TablePointer tablePointer,
      String entityGroup,
      String entityA,
      String entityB,
      String entityAIdFieldName,
      String entityBIdFieldName) {
    super(tablePointer);
    this.entityGroup = entityGroup;
    this.entityA = entityA;
    this.entityB = entityB;
    this.entityAIdColumnSchema = new ColumnSchema(entityAIdFieldName, CellValue.SQLDataType.INT64);
    this.entityBIdColumnSchema = new ColumnSchema(entityBIdFieldName, CellValue.SQLDataType.INT64);
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
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

  public FieldPointer getEntityAIdField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(entityAIdColumnSchema.getColumnName())
        .build();
  }

  public ColumnSchema getEntityAIdColumnSchema() {
    return entityAIdColumnSchema;
  }

  public FieldPointer getEntityBIdField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(entityBIdColumnSchema.getColumnName())
        .build();
  }

  public ColumnSchema getEntityBIdColumnSchema() {
    return entityBIdColumnSchema;
  }
}
