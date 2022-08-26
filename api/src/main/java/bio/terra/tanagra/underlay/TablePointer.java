package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFTablePointer;
import com.google.common.base.Strings;
import java.util.List;

public class TablePointer {
  private final DataPointer dataPointer;
  private final String tableName;
  private final TableFilter tableFilter;

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
    if (Strings.isNullOrEmpty(serialized.getTable())) {
      throw new IllegalArgumentException("Table name not defined");
    }

    TablePointer tablePointer = new TablePointer(serialized.getTable(), dataPointer);
    if (serialized.getFilter() == null) {
      return tablePointer;
    } else {
      TableFilter tableFilter = serialized.getFilter().deserializeToInternal(tablePointer);
      return new TablePointer(serialized.getTable(), dataPointer, tableFilter);
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

  public String getPathForIndexing() {
    return dataPointer.getTablePathForIndexing(tableName);
  }

  public FilterVariable getFilterVariable(TableVariable tableVariable, List<TableVariable> tables) {
    if (!hasTableFilter()) {
      return null;
    }
    return getTableFilter().buildVariable(tableVariable, tables);
  }
}
