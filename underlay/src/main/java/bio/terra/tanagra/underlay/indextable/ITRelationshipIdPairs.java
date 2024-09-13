package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.underlay.*;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.sourcetable.*;
import com.google.common.collect.ImmutableList;
import java.util.*;
import java.util.stream.*;

public final class ITRelationshipIdPairs extends IndexTable {
  private static final String TABLE_NAME = "RIDS";

  private final String entityGroup;
  private final String entityA;
  private final String entityB;
  private final STRelationshipIdPairs sourceTable;

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
    return sourceTable == null
        ? ImmutableList.copyOf(
            Arrays.stream(Column.values()).map(Column::getSchema).collect(Collectors.toList()))
        : sourceTable.getColumnSchemas();
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
