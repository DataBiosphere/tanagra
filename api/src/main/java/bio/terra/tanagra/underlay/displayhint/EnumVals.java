package bio.terra.tanagra.underlay.displayhint;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFDisplayHint;
import bio.terra.tanagra.serialization.displayhint.UFEnumVals;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.FieldPointer;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.underlay.TablePointer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EnumVals extends DisplayHint {
  private static final Logger LOGGER = LoggerFactory.getLogger(EnumVals.class);
  private static final String ENUM_VALUE_COLUMN_ALIAS = "enumVal";
  private static final int MAX_ENUM_VALS_FOR_DISPLAY_HINT = 100;

  private final List<ValueDisplay> valueDisplays;

  public EnumVals(List<ValueDisplay> valueDisplays) {
    this.valueDisplays = valueDisplays;
  }

  public static EnumVals fromSerialized(UFEnumVals serialized) {
    if (serialized.getValueDisplays() == null) {
      throw new IllegalArgumentException("Enum values map is undefined");
    }
    List<ValueDisplay> valueDisplays =
        serialized.getValueDisplays().stream()
            .map(vd -> ValueDisplay.fromSerialized(vd))
            .collect(Collectors.toList());
    return new EnumVals(valueDisplays);
  }

  @Override
  public Type getType() {
    return Type.ENUM;
  }

  @Override
  public UFDisplayHint serialize() {
    return new UFEnumVals(this);
  }

  public List<ValueDisplay> getValueDisplays() {
    return Collections.unmodifiableList(valueDisplays);
  }

  /**
   * Build a query to fetch a set of distinct values, up to the maximum allowed. e.g.
   *
   * <p>SELECT c.standard_concept AS enumVal
   *
   * <p>FROM concept AS c
   *
   * <p>GROUP BY c.standard_concept
   *
   * <p>ORDER BY c.standard_concept
   *
   * <p>LIMIT 101
   */
  public static EnumVals computeForField(Literal.DataType dataType, FieldPointer value) {
    Query query = queryPossibleEnumVals(value);
    List<ColumnSchema> columnSchemas =
        List.of(
            new ColumnSchema(
                ENUM_VALUE_COLUMN_ALIAS, CellValue.SQLDataType.fromUnderlayDataType(dataType)));

    DataPointer dataPointer = value.getTablePointer().getDataPointer();
    QueryRequest queryRequest =
        new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    List<String> enumStringVals = new ArrayList<>();
    Iterator<RowResult> rowResultIter = queryResult.getRowResults().iterator();
    while (rowResultIter.hasNext()) {
      enumStringVals.add(
          rowResultIter.next().get(ENUM_VALUE_COLUMN_ALIAS).getString().orElse(null));
      if (enumStringVals.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
        // if there are more than the max number of values, then skip the display hint
        LOGGER.info(
            "Skipping enum values display hint because there are >{} possible values: {}",
            MAX_ENUM_VALS_FOR_DISPLAY_HINT,
            value.getColumnName());
        return null;
      }
    }
    return new EnumVals(
        enumStringVals.stream().map(esv -> new ValueDisplay(esv)).collect(Collectors.toList()));
  }

  /**
   * Build a query to fetch a set of distinct values and their display strings, up to the maximum
   * allowed. e.g.
   *
   * <p>SELECT x.enumVal, v.vocabulary_name AS enumDisplay
   *
   * <p>FROM (SELECT c.vocabulary_id AS enumVal FROM concept GROUP BY c.vocabulary_id ORDER BY
   * c.vocabulary_id) AS x
   *
   * <p>JOIN vocabulary as v
   *
   * <p>ON v.id = x.enumVal
   *
   * <p>GROUP BY x.enumVal
   *
   * <p>ORDER BY x.enumVal
   *
   * <p>LIMIT 101
   */
  public static EnumVals computeForField(
      Literal.DataType dataType, FieldPointer value, FieldPointer display) {
    Query possibleValuesQuery = queryPossibleEnumVals(value);
    DataPointer dataPointer = value.getTablePointer().getDataPointer();
    TablePointer possibleValsTable =
        TablePointer.fromRawSql(possibleValuesQuery.renderSQL(), dataPointer);
    FieldPointer possibleValField =
        new FieldPointer.Builder()
            .tablePointer(possibleValsTable)
            .columnName(ENUM_VALUE_COLUMN_ALIAS)
            .build();
    FieldPointer possibleDisplayField =
        new FieldPointer.Builder()
            .tablePointer(possibleValsTable)
            .columnName(ENUM_VALUE_COLUMN_ALIAS)
            .foreignTablePointer(display.getForeignTablePointer())
            .foreignKeyColumnName(display.getForeignKeyColumnName())
            .foreignColumnName(display.getForeignColumnName())
            .sqlFunctionWrapper(display.getSqlFunctionWrapper())
            .build();

    // build the outer query for the list of (possible value, display) pairs
    List<TableVariable> tables = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(possibleValsTable);
    tables.add(primaryTable);

    final String enumDisplayColumnAlias = "enumDisplay";
    FieldVariable valueFieldVar =
        possibleValField.buildVariable(primaryTable, tables, ENUM_VALUE_COLUMN_ALIAS);
    FieldVariable displayFieldVar =
        possibleDisplayField.buildVariable(primaryTable, tables, enumDisplayColumnAlias);
    Query query =
        new Query.Builder()
            .select(List.of(valueFieldVar, displayFieldVar))
            .tables(tables)
            .orderBy(List.of(displayFieldVar))
            .limit(MAX_ENUM_VALS_FOR_DISPLAY_HINT + 1)
            .build();

    LOGGER.info(
        "SQL data type of value is: {}", CellValue.SQLDataType.fromUnderlayDataType(dataType));
    List<ColumnSchema> columnSchemas =
        List.of(
            new ColumnSchema(
                ENUM_VALUE_COLUMN_ALIAS, CellValue.SQLDataType.fromUnderlayDataType(dataType)),
            new ColumnSchema(enumDisplayColumnAlias, CellValue.SQLDataType.STRING));

    // run the query
    QueryRequest queryRequest =
        new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    // iterate through the query results, building the list of enum values
    List<ValueDisplay> valueDisplays = new ArrayList<>();
    Iterator<RowResult> rowResultIter = queryResult.getRowResults().iterator();
    while (rowResultIter.hasNext()) {
      RowResult rowResult = rowResultIter.next();
      CellValue cellValue = rowResult.get(ENUM_VALUE_COLUMN_ALIAS);
      valueDisplays.add(
          new ValueDisplay(
              cellValue.getLiteral(),
              rowResult.get(enumDisplayColumnAlias).getString().orElse(null)));
      if (valueDisplays.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
        // if there are more than the max number of values, then skip the display hint
        LOGGER.info(
            "Skipping enum values display hint because there are >{} possible values: {}",
            MAX_ENUM_VALS_FOR_DISPLAY_HINT,
            valueFieldVar.getAlias());
        return null;
      }
    }
    return new EnumVals(valueDisplays);
  }

  private static Query queryPossibleEnumVals(FieldPointer value) {
    List<TableVariable> nestedQueryTables = new ArrayList<>();
    TableVariable nestedPrimaryTable = TableVariable.forPrimary(value.getTablePointer());
    nestedQueryTables.add(nestedPrimaryTable);

    final String possibleValAlias = ENUM_VALUE_COLUMN_ALIAS;
    FieldVariable nestedValueFieldVar =
        value.buildVariable(nestedPrimaryTable, nestedQueryTables, possibleValAlias);
    return new Query.Builder()
        .select(List.of(nestedValueFieldVar))
        .tables(nestedQueryTables)
        .orderBy(List.of(nestedValueFieldVar))
        .groupBy(List.of(nestedValueFieldVar))
        .build();
  }
}
