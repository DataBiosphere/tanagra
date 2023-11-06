package bio.terra.tanagra.underlay2.indextable;

import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.NameHelper;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class IndexTable {
  protected final NameHelper namer;
  protected final DataPointer dataPointer;

  protected IndexTable(NameHelper namer, DataPointer dataPointer) {
    this.namer = namer;
    this.dataPointer = dataPointer;
  }

  public abstract String getTableBaseName();

  public TablePointer getTablePointer() {
    return TablePointer.fromTableName(getTableBaseName(), dataPointer);
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
