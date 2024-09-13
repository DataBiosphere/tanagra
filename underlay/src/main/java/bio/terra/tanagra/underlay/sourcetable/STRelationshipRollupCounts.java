package bio.terra.tanagra.underlay.sourcetable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.underlay.ColumnSchema;
import com.google.common.collect.ImmutableList;

public class STRelationshipRollupCounts extends SourceTable {
  private final String entityGroup;
  private final String entity;
  private final String countedEntity;
  private final ColumnSchema entityIdColumnSchema;
  private final ColumnSchema countColumnSchema;

  public STRelationshipRollupCounts(
      BQTable bqTable,
      String entityGroup,
      String entity,
      String countedEntity,
      String entityIdFieldName,
      String countFieldName) {
    super(bqTable);
    this.entityGroup = entityGroup;
    this.entity = entity;
    this.countedEntity = countedEntity;
    this.entityIdColumnSchema = new ColumnSchema(entityIdFieldName, DataType.INT64);
    this.countColumnSchema = new ColumnSchema(countFieldName, DataType.INT64);
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return ImmutableList.of(entityIdColumnSchema, countColumnSchema);
  }

  public String getEntityGroup() {
    return entityGroup;
  }

  public String getEntity() {
    return entity;
  }

  public String getCountedEntity() {
    return countedEntity;
  }

  public SqlField getEntityIdField() {
    return SqlField.of(entityIdColumnSchema.getColumnName());
  }

  public SqlField getCountField() {
    return SqlField.of(countColumnSchema.getColumnName());
  }
}
