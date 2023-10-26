package bio.terra.tanagra.underlay2.sourcetable;

import bio.terra.tanagra.query.*;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class SourceTable {
  protected TablePointer tablePointer;

  protected SourceTable(TablePointer tablePointer) {
    this.tablePointer = tablePointer;
  }

  public TablePointer getTablePointer() {
    return tablePointer;
  }

  public abstract ImmutableList<ColumnSchema> getColumnSchemas();

  public Query getQueryAll(Map<ColumnSchema, String> columnAliases) {
    TableVariable primaryTable = TableVariable.forPrimary(getTablePointer());
    List<TableVariable> tableVars = List.of(primaryTable);
    List<FieldVariable> select =
        getColumnSchemas().stream()
            .map(
                columnSchema ->
                    new FieldPointer.Builder()
                        .columnName(columnSchema.getColumnName())
                        .tablePointer(getTablePointer())
                        .build()
                        .buildVariable(primaryTable, tableVars, columnAliases.get(columnSchema)))
            .collect(Collectors.toList());
    return new Query.Builder().select(select).tables(tableVars).build();
  }
}
