package bio.terra.tanagra.underlay2.indexschema;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import java.util.*;
import java.util.stream.Collectors;

public final class HierarchyChildParent {
  private HierarchyChildParent() {}

  public static final String TABLE_NAME = "HCP";

  public enum Column {
    CHILD(new ColumnSchema("child", CellValue.SQLDataType.INT64)),
    PARENT(new ColumnSchema("parent", CellValue.SQLDataType.INT64));

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

  public static TablePointer getTable(String entity, String hierarchy) {
    String tableName =
        SchemaUtils.getSingleton()
            .getReservedTableName(TABLE_NAME + "_" + entity + "_" + hierarchy);
    return SchemaUtils.getSingleton().getIndexTable(tableName);
  }

  public static FieldPointer getChildField(String entity, String hierarchy) {
    return new FieldPointer.Builder()
        .tablePointer(getTable(entity, hierarchy))
        .columnName(Column.CHILD.getSchema().getColumnName())
        .build();
  }

  public static FieldPointer getParentField(String entity, String hierarchy) {
    return new FieldPointer.Builder()
        .tablePointer(getTable(entity, hierarchy))
        .columnName(Column.PARENT.getSchema().getColumnName())
        .build();
  }
}
