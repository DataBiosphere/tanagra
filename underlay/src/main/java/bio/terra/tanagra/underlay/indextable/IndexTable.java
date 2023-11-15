package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class IndexTable {
  protected final NameHelper namer;
  protected final SZBigQuery.IndexData bigQueryConfig;

  protected IndexTable(NameHelper namer, SZBigQuery.IndexData bigQueryConfig) {
    this.namer = namer;
    this.bigQueryConfig = bigQueryConfig;
  }

  public abstract String getTableBaseName();

  public TablePointer getTablePointer() {
    return new TablePointer(
        bigQueryConfig.projectId,
        bigQueryConfig.datasetId,
        namer.getReservedTableName(getTableBaseName()));
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
