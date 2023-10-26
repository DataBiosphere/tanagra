package bio.terra.tanagra.underlay2.indextable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.NameHelper;
import com.google.common.collect.ImmutableList;
import java.util.*;
import java.util.stream.Collectors;

public final class ITRelationshipIdPairs extends IndexTable {
  private static final String TABLE_NAME = "RIDS";

  private final String entityGroup;
  private final String entityA;
  private final String entityB;

  public ITRelationshipIdPairs(
      NameHelper namer,
      DataPointer dataPointer,
      String entityGroup,
      String entityA,
      String entityB) {
    super(namer, dataPointer);
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
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    // Columns are static and don't depend on the entity.
    return ImmutableList.copyOf(
        Arrays.stream(Column.values())
            .map(column -> column.getSchema())
            .collect(Collectors.toList()));
  }

  public FieldPointer getEntityAIdField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.ENTITY_A_ID.getSchema().getColumnName())
        .build();
  }

  public FieldPointer getEntityBIdField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(Column.ENTITY_B_ID.getSchema().getColumnName())
        .build();
  }

  public FieldPointer getEntityIdField(String entity) {
    return entity.equals(entityA) ? getEntityAIdField() : getEntityBIdField();
  }

  public enum Column {
    ENTITY_A_ID(new ColumnSchema("entity_A_id", CellValue.SQLDataType.INT64)),
    ENTITY_B_ID(new ColumnSchema("entity_B_id", CellValue.SQLDataType.INT64));

    private final ColumnSchema schema;

    Column(ColumnSchema schema) {
      this.schema = schema;
    }

    public ColumnSchema getSchema() {
      return schema;
    }
  }
}
