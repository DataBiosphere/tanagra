package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFTablePointer;
import com.google.common.base.Strings;

public class TablePointer {
  private DataPointer dataPointer;
  private String tableName;
  private TableFilter tableFilter;

  public TablePointer(String tableName, DataPointer dataPointer) {
    this.dataPointer = dataPointer;
    this.tableName = tableName;
    this.tableFilter = null;
  }

  public TablePointer(String tableName, DataPointer dataPointer, TableFilter tableFilter) {
    this.dataPointer = dataPointer;
    this.tableName = tableName;
    this.tableFilter = tableFilter;
  }

  public static TablePointer fromSerialized(UFTablePointer serialized, DataPointer dataPointer) {
    if (Strings.isNullOrEmpty(serialized.table)) {
      throw new IllegalArgumentException("Table name not defined");
    }

    TablePointer tablePointer = new TablePointer(serialized.table, dataPointer);
    if (serialized.filter == null) {
      return tablePointer;
    } else {
      TableFilter tableFilter = serialized.filter.deserializeToInternal(tablePointer);
      return new TablePointer(serialized.table, dataPointer, tableFilter);
    }
  }

  public DataPointer getDataPointer() {
    return dataPointer;
  }

  public String getTableName() {
    return tableName;
  }

  public boolean hasTableFilter() {
    return tableFilter != null;
  }

  public TableFilter getTableFilter() {
    return tableFilter;
  }

  public String getSQL() {
    return dataPointer.getTableSQL(tableName);
  }
}
