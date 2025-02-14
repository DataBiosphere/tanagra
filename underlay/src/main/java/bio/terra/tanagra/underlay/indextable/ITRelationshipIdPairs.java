package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.SourceSchema;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.sourcetable.STRelationshipIdPairs;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class ITRelationshipIdPairs extends IndexTable {
  private static final String TABLE_NAME = "RIDS";
  private final String entityGroup;
  private final String entityA;
  private final String entityB;
  private final STRelationshipIdPairs sourceTable;
  private final ImmutableList<ColumnSchema> columnSchemas;

  public ITRelationshipIdPairs(
      NameHelper namer,
      SZBigQuery.IndexData bigQueryConfig,
      String entityGroup,
      String entityA,
      String entityB) {
    super(namer, bigQueryConfig);
    this.entityGroup = entityGroup;
    this.entityA = entityA;
    this.entityB = entityB;
    this.sourceTable = null;
    this.columnSchemas =
        ImmutableList.copyOf(
            Arrays.stream(Column.values()).map(Column::getSchema).collect(Collectors.toList()));
  }

  public ITRelationshipIdPairs(
      SourceSchema sourceSchema, String entityGroup, String entityA, String entityB) {
    super(
        sourceSchema
            .getRelationshipIdPairs(entityGroup, entityA, entityB)
            .getTablePointer()
            .getSql());
    this.entityGroup = entityGroup;
    this.entityA = entityA;
    this.entityB = entityB;
    this.sourceTable = sourceSchema.getRelationshipIdPairs(entityGroup, entityA, entityB);
    this.columnSchemas = sourceTable.getColumnSchemas();
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

  @Override
  public String getTableBaseName() {
    return TABLE_NAME + "_" + entityGroup + "_" + entityA + "_" + entityB;
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return columnSchemas;
  }

  public SqlField getEntityAIdField() {
    return sourceTable == null
        ? SqlField.of(Column.ENTITY_A_ID.getSchema().getColumnName())
        : sourceTable.getEntityAIdField();
  }

  public SqlField getEntityBIdField() {
    return sourceTable == null
        ? SqlField.of(Column.ENTITY_B_ID.getSchema().getColumnName())
        : sourceTable.getEntityBIdField();
  }

  public SqlField getEntityIdField(String entity) {
    return entity.equals(entityA) ? getEntityAIdField() : getEntityBIdField();
  }

  @Override
  public boolean isGeneratedIndexTable() {
    // TODO: Support using the source table instead of a generated index table for all IT* classes.
    return sourceTable == null;
  }

  public enum Column {
    ENTITY_A_ID(new ColumnSchema("entity_A_id", DataType.INT64)),
    ENTITY_B_ID(new ColumnSchema("entity_B_id", DataType.INT64));

    private final ColumnSchema schema;

    Column(ColumnSchema schema) {
      this.schema = schema;
    }

    public ColumnSchema getSchema() {
      return schema;
    }
  }
}
