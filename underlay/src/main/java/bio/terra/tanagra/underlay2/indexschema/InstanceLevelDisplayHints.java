package bio.terra.tanagra.underlay2.indexschema;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.TablePointer;
import java.util.*;
import java.util.stream.Collectors;

public final class InstanceLevelDisplayHints {
  private InstanceLevelDisplayHints() {}

  public static final String TABLE_NAME = "ILDH";

  public enum Column {
    ENTITY_ID(new ColumnSchema("entity_id", CellValue.SQLDataType.INT64, true)),
    ATTRIBUTE_NAME(new ColumnSchema("attribute_name", CellValue.SQLDataType.STRING, true)),
    MIN(new ColumnSchema("min", CellValue.SQLDataType.FLOAT)),
    MAX(new ColumnSchema("max", CellValue.SQLDataType.FLOAT)),
    ENUM_VALUE(new ColumnSchema("enum_value", CellValue.SQLDataType.INT64)),
    ENUM_DISPLAY(new ColumnSchema("enum_display", CellValue.SQLDataType.STRING)),
    ENUM_COUNT(new ColumnSchema("enum_count", CellValue.SQLDataType.INT64));

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

  public static TablePointer getTable(String idEntity, String attributeEntity) {
    String tableName =
        SchemaUtils.getSingleton()
            .getReservedTableName(TABLE_NAME + "_" + idEntity + "_" + attributeEntity);
    return SchemaUtils.getSingleton().getIndexTable(tableName);
  }
}
