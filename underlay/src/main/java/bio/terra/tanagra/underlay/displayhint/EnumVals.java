package bio.terra.tanagra.underlay.displayhint;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFDisplayHint;
import bio.terra.tanagra.serialization.displayhint.UFEnumVals;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EnumVals extends DisplayHint {
  private static final Logger LOGGER = LoggerFactory.getLogger(EnumVals.class);
  private static final String ENUM_VALUE_COLUMN_ALIAS = "enumVal";
  private static final String ENUM_COUNT_COLUMN_ALIAS = "enumCount";
  private static final String ENUM_DISPLAY_COLUMN_ALIAS = "enumDisplay";
  private static final int MAX_ENUM_VALS_FOR_DISPLAY_HINT = 100;

  private final List<EnumVal> enumValsList;

  public EnumVals(List<EnumVal> enumValsList) {
    this.enumValsList = enumValsList;
  }

  public static EnumVals fromSerialized(UFEnumVals serialized) {
    if (serialized.getEnumVals() == null) {
      throw new InvalidConfigException("Enum values list is undefined");
    }
    List<EnumVal> enumVals =
        serialized.getEnumVals().stream()
            .map(ev -> EnumVal.fromSerialized(ev))
            .collect(Collectors.toList());
    return new EnumVals(enumVals);
  }

  @Override
  public Type getType() {
    return Type.ENUM;
  }

  @Override
  public UFDisplayHint serialize() {
    return new UFEnumVals(this);
  }

  public List<EnumVal> getEnumValsList() {
    return Collections.unmodifiableList(enumValsList);
  }

  public String getEnumDisplay(Literal enumValue) {
    Optional<EnumVal> enumDisplay =
        enumValsList.stream()
            .filter(ev -> ev.getValueDisplay().getValue().equals(enumValue))
            .findFirst();
    if (enumDisplay.isEmpty()) {
      LOGGER.warn("Enum display not found for value: {}", enumValue.toString());
      return null;
    } else {
      return enumDisplay.get().getValueDisplay().getDisplay();
    }
  }

  /**
   * Build a query to fetch a set of distinct values, up to the maximum allowed. e.g.
   *
   * <p>SELECT c.standard_concept AS enumVal, count(*) AS enumCount
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
                ENUM_VALUE_COLUMN_ALIAS, CellValue.SQLDataType.fromUnderlayDataType(dataType)),
            new ColumnSchema(ENUM_COUNT_COLUMN_ALIAS, CellValue.SQLDataType.INT64));

    DataPointer dataPointer = value.getTablePointer().getDataPointer();
    QueryRequest queryRequest =
        new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    List<EnumVal> enumVals = new ArrayList<>();
    Iterator<RowResult> rowResultIter = queryResult.getRowResults().iterator();
    while (rowResultIter.hasNext()) {
      RowResult rowResult = rowResultIter.next();
      String val = rowResult.get(ENUM_VALUE_COLUMN_ALIAS).getString().orElse(null);
      long count = rowResult.get(ENUM_COUNT_COLUMN_ALIAS).getLong().getAsLong();
      enumVals.add(new EnumVal(new ValueDisplay(val), count));
      if (enumVals.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
        // if there are more than the max number of values, then skip the display hint
        LOGGER.info(
            "Skipping enum values display hint because there are >{} possible values: {}",
            MAX_ENUM_VALS_FOR_DISPLAY_HINT,
            value.getColumnName());
        return null;
      }
    }
    return new EnumVals(enumVals);
  }

  /**
   * Build a query to fetch a set of distinct values and their display strings, up to the maximum
   * allowed. e.g.
   *
   * <p>SELECT x.enumVal, x.enumCount, v.vocabulary_name AS enumDisplay
   *
   * <p>FROM (SELECT c.vocabulary_id AS enumVal, count(*) AS enumCount FROM concept GROUP BY
   * c.vocabulary_id ORDER BY c.vocabulary_id) AS x
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
    if (!display.isForeignKey() && !display.hasSqlFunctionWrapper()) {
      throw new UnsupportedOperationException(
          "Display fields that are not foreign key fields are not yet supported: "
              + value.getColumnName()
              + ", "
              + display.getColumnName());
    }

    Query possibleValuesQuery = queryPossibleEnumVals(value);
    DataPointer dataPointer = value.getTablePointer().getDataPointer();
    TablePointer possibleValsTable =
        TablePointer.fromRawSql(possibleValuesQuery.renderSQL(), dataPointer);
    FieldPointer possibleValField =
        new FieldPointer.Builder()
            .tablePointer(possibleValsTable)
            .columnName(ENUM_VALUE_COLUMN_ALIAS)
            .build();
    FieldPointer possibleCountField =
        new FieldPointer.Builder()
            .tablePointer(possibleValsTable)
            .columnName(ENUM_COUNT_COLUMN_ALIAS)
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

    FieldVariable valueFieldVar =
        possibleValField.buildVariable(primaryTable, tables, ENUM_VALUE_COLUMN_ALIAS);
    FieldVariable countFieldVar =
        possibleCountField.buildVariable(primaryTable, tables, ENUM_COUNT_COLUMN_ALIAS);
    FieldVariable displayFieldVar =
        possibleDisplayField.buildVariable(primaryTable, tables, ENUM_DISPLAY_COLUMN_ALIAS);
    Query query =
        new Query.Builder()
            .select(List.of(valueFieldVar, countFieldVar, displayFieldVar))
            .tables(tables)
            .orderBy(List.of(new OrderByVariable(displayFieldVar)))
            .limit(MAX_ENUM_VALS_FOR_DISPLAY_HINT + 1)
            .build();

    LOGGER.info(
        "SQL data type of value is: {}", CellValue.SQLDataType.fromUnderlayDataType(dataType));
    List<ColumnSchema> columnSchemas =
        List.of(
            new ColumnSchema(
                ENUM_VALUE_COLUMN_ALIAS, CellValue.SQLDataType.fromUnderlayDataType(dataType)),
            new ColumnSchema(ENUM_COUNT_COLUMN_ALIAS, CellValue.SQLDataType.INT64),
            new ColumnSchema(ENUM_DISPLAY_COLUMN_ALIAS, CellValue.SQLDataType.STRING));

    // run the query
    QueryRequest queryRequest =
        new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    // iterate through the query results, building the list of enum values
    List<EnumVal> enumVals = new ArrayList<>();
    Iterator<RowResult> rowResultIter = queryResult.getRowResults().iterator();
    while (rowResultIter.hasNext()) {
      RowResult rowResult = rowResultIter.next();
      CellValue cellValue = rowResult.get(ENUM_VALUE_COLUMN_ALIAS);
      enumVals.add(
          new EnumVal(
              new ValueDisplay(
                  // TODO: Make a static NULL Literal instance, instead of overloading the String
                  // value.
                  cellValue.getLiteral().orElse(new Literal((String) null)),
                  rowResult.get(ENUM_DISPLAY_COLUMN_ALIAS).getString().orElse(null)),
              rowResult.get(ENUM_COUNT_COLUMN_ALIAS).getLong().getAsLong()));
      if (enumVals.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
        // if there are more than the max number of values, then skip the display hint
        LOGGER.info(
            "Skipping enum values display hint because there are >{} possible values: {}",
            MAX_ENUM_VALS_FOR_DISPLAY_HINT,
            valueFieldVar.getAlias());
        return null;
      }
    }

    // Sort the enum values, so the expanded config has a consistent ordering.
    enumVals.sort(Comparator.comparing(ev -> String.valueOf(ev.getValueDisplay().getDisplay())));

    return new EnumVals(enumVals);
  }

  private static Query queryPossibleEnumVals(FieldPointer value) {
    List<TableVariable> nestedQueryTables = new ArrayList<>();
    TableVariable nestedPrimaryTable = TableVariable.forPrimary(value.getTablePointer());
    nestedQueryTables.add(nestedPrimaryTable);

    FieldVariable nestedValueFieldVar =
        value.buildVariable(nestedPrimaryTable, nestedQueryTables, ENUM_VALUE_COLUMN_ALIAS);

    FieldPointer countFieldPointer = value.toBuilder().sqlFunctionWrapper("COUNT").build();
    FieldVariable nestedCountFieldVar =
        countFieldPointer.buildVariable(
            nestedPrimaryTable, nestedQueryTables, ENUM_COUNT_COLUMN_ALIAS);

    return new Query.Builder()
        .select(List.of(nestedValueFieldVar, nestedCountFieldVar))
        .tables(nestedQueryTables)
        .orderBy(List.of(new OrderByVariable(nestedValueFieldVar)))
        .groupBy(List.of(nestedValueFieldVar))
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnumVals enumVals = (EnumVals) o;
    return enumValsList.equals(enumVals.enumValsList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enumValsList);
  }
}
