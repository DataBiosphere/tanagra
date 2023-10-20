package bio.terra.tanagra.underlay.displayhint;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.serialization.UFDisplayHint;
import bio.terra.tanagra.serialization.displayhint.UFNumericRange;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.DisplayHint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NumericRange extends DisplayHint {
  private final Double minVal;
  private final Double maxVal;

  public NumericRange(Double minVal, Double maxVal) {
    this.minVal = minVal;
    this.maxVal = maxVal;
  }

  public static NumericRange fromSerialized(UFNumericRange serialized) {
    if (serialized.getMinVal() == null) {
      throw new InvalidConfigException("Numeric range minimum value is undefined");
    }
    if (serialized.getMaxVal() == null) {
      throw new InvalidConfigException("Numeric range maximum value is undefined");
    }
    return new NumericRange(serialized.getMinVal(), serialized.getMaxVal());
  }

  @Override
  public Type getType() {
    return Type.RANGE;
  }

  @Override
  public UFDisplayHint serialize() {
    return new UFNumericRange(this);
  }

  public Double getMinVal() {
    return minVal;
  }

  public Double getMaxVal() {
    return maxVal;
  }

  public static NumericRange computeForField(FieldPointer value, Literal.DataType dataType) {
    // build the nested query for the possible values
    List<TableVariable> nestedQueryTables = new ArrayList<>();
    TableVariable nestedPrimaryTable = TableVariable.forPrimary(value.getTablePointer());
    nestedQueryTables.add(nestedPrimaryTable);

    final String possibleValAlias = "possibleVal";
    FieldVariable nestedValueFieldVar =
        value.buildVariable(nestedPrimaryTable, nestedQueryTables, possibleValAlias);
    Query possibleValuesQuery =
        new Query.Builder().select(List.of(nestedValueFieldVar)).tables(nestedQueryTables).build();

    DataPointer dataPointer = value.getTablePointer().getDataPointer();
    TablePointer possibleValsTable =
        TablePointer.fromRawSql(possibleValuesQuery.renderSQL(), dataPointer);

    // build the outer query for the list of (possible value, display) pairs
    List<TableVariable> tables = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(possibleValsTable);
    tables.add(primaryTable);

    final String minValAlias = "minVal";
    final String maxValAlias = "maxVal";

    List<FieldVariable> select = new ArrayList<>();
    FieldPointer minVal =
        new FieldPointer.Builder()
            .tablePointer(possibleValsTable)
            .columnName(possibleValAlias)
            .sqlFunctionWrapper("MIN")
            .build();
    select.add(minVal.buildVariable(primaryTable, tables, minValAlias));
    FieldPointer maxVal =
        new FieldPointer.Builder()
            .tablePointer(possibleValsTable)
            .columnName(possibleValAlias)
            .sqlFunctionWrapper("MAX")
            .build();
    select.add(maxVal.buildVariable(primaryTable, tables, maxValAlias));
    Query query = new Query.Builder().select(select).tables(tables).build();

    List<ColumnSchema> columnSchemas =
        List.of(
            new ColumnSchema(minValAlias, CellValue.SQLDataType.fromUnderlayDataType(dataType)),
            new ColumnSchema(maxValAlias, CellValue.SQLDataType.fromUnderlayDataType(dataType)));

    QueryRequest queryRequest =
        new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);
    RowResult rowResult = queryResult.getSingleRowResult();
    return new NumericRange(
        rowResult.get(minValAlias).getDouble().getAsDouble(),
        rowResult.get(maxValAlias).getDouble().getAsDouble());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NumericRange that = (NumericRange) o;
    return minVal.equals(that.minVal) && maxVal.equals(that.maxVal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(minVal, maxVal);
  }
}
