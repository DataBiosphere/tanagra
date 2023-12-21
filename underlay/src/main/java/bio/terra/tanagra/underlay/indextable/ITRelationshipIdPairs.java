package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class ITRelationshipIdPairs extends IndexTable {
  private static final String TABLE_NAME = "RIDS";

  private final String entityGroup;
  private final String entityA;
  private final String entityB;

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
  public ImmutableList<SqlColumnSchema> getColumnSchemas() {
    // Columns are static and don't depend on the entity.
    return ImmutableList.copyOf(
        Arrays.stream(Column.values())
            .map(column -> column.getSchema())
            .collect(Collectors.toList()));
  }

  public SqlField getEntityAIdField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.ENTITY_A_ID.getSchema().getColumnName())
        .build();
  }

  public SqlField getEntityBIdField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.ENTITY_B_ID.getSchema().getColumnName())
        .build();
  }

  public SqlField getEntityIdField(String entity) {
    return entity.equals(entityA) ? getEntityAIdField() : getEntityBIdField();
  }

  public enum Column {
    ENTITY_A_ID(new SqlColumnSchema("entity_A_id", DataType.INT64)),
    ENTITY_B_ID(new SqlColumnSchema("entity_B_id", DataType.INT64));

    private final SqlColumnSchema schema;

    Column(SqlColumnSchema schema) {
      this.schema = schema;
    }

    public SqlColumnSchema getSchema() {
      return schema;
    }
  }
}
