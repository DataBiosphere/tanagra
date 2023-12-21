package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.underlay.ColumnSchema;
import com.google.common.collect.ImmutableList;

public class STRelationshipIdPairs extends SourceTable {
  private final String entityGroup;
  private final String entityA;
  private final String entityB;
  private final ColumnSchema entityAIdColumnSchema;
  private final ColumnSchema entityBIdColumnSchema;

  public STRelationshipIdPairs(
      BQTable bqTable,
      String entityGroup,
      String entityA,
      String entityB,
      String entityAIdFieldName,
      String entityBIdFieldName) {
    super(bqTable);
    this.entityGroup = entityGroup;
    this.entityA = entityA;
    this.entityB = entityB;
    this.entityAIdColumnSchema = new ColumnSchema(entityAIdFieldName, DataType.INT64);
    this.entityBIdColumnSchema = new ColumnSchema(entityBIdFieldName, DataType.INT64);
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

  public SqlField getEntityAIdField() {
    return SqlField.of(entityAIdColumnSchema.getColumnName());
  }

  public SqlField getEntityBIdField() {
    return SqlField.of(entityBIdColumnSchema.getColumnName());
  }
}
