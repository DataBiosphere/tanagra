package bio.terra.tanagra.underlay2.indexschema;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import java.util.*;
import java.util.stream.Collectors;

public final class HierarchyAncestorDescendant {
  private HierarchyAncestorDescendant() {}

  public static final String TABLE_NAME = "HAD";

  public enum Column {
    ANCESTOR(new ColumnSchema("ancestor", CellValue.SQLDataType.INT64)),
    DESCENDANT(new ColumnSchema("descendant", CellValue.SQLDataType.INT64));

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

  public static FieldPointer getAncestorField(String entity, String hierarchy) {
    return new FieldPointer.Builder()
        .tablePointer(getTable(entity, hierarchy))
        .columnName(Column.ANCESTOR.getSchema().getColumnName())
        .build();
  }

  public static FieldPointer getDescendantField(String entity, String hierarchy) {
    return new FieldPointer.Builder()
        .tablePointer(getTable(entity, hierarchy))
        .columnName(Column.DESCENDANT.getSchema().getColumnName())
        .build();
  }
}
