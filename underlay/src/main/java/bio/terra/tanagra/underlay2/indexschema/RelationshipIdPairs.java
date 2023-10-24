package bio.terra.tanagra.underlay2.indexschema;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import java.util.*;
import java.util.stream.Collectors;

public final class RelationshipIdPairs {
  private RelationshipIdPairs() {}

  public static final String TABLE_NAME = "RIDS";

  public enum Column {
    SELECT_ENTITY_ID(new ColumnSchema("select_entity_id", CellValue.SQLDataType.INT64)),
    WHERE_ENTITY_ID(new ColumnSchema("where_entity_id", CellValue.SQLDataType.INT64));

    private final ColumnSchema schema;

    Column(ColumnSchema schema) {
      this.schema = schema;
    }

    public ColumnSchema getSchema() {
      return schema;
    }
  }

  public static List<ColumnSchema> getColumns() {
    return Arrays.asList(Column.values()).stream()
        .map(Column::getSchema)
        .collect(Collectors.toList());
  }

  public static TablePointer getTable(String selectEntity, String whereEntity) {
    String tableName =
        SchemaUtils.getSingleton()
            .getReservedTableName(TABLE_NAME + "_" + selectEntity + "_" + whereEntity);
    return SchemaUtils.getSingleton().getIndexTable(tableName);
  }

  public static FieldPointer getSelectEntityIdField(String selectEntity, String whereEntity) {
    return new FieldPointer.Builder()
        .tablePointer(getTable(selectEntity, whereEntity))
        .columnName(Column.SELECT_ENTITY_ID.getSchema().getColumnName())
        .build();
  }

  public static FieldPointer getWhereEntityIdField(String selectEntity, String whereEntity) {
    return new FieldPointer.Builder()
        .tablePointer(getTable(selectEntity, whereEntity))
        .columnName(Column.WHERE_ENTITY_ID.getSchema().getColumnName())
        .build();
  }
}
