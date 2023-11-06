package bio.terra.tanagra.underlay.displayhint;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.OrderByVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFDisplayHint;
import bio.terra.tanagra.serialization.displayhint.UFEnumVals;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.ValueDisplay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
    return enumValsList == null
        ? Collections.emptyList()
        : Collections.unmodifiableList(enumValsList);
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
  public static EnumVals computeForField(
      FieldPointer countedIdField, Literal.DataType dataType, FieldPointer value) {
    return new EnumVals(queryPossibleEnumVals(countedIdField, dataType, value, null));
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
      FieldPointer countedIdField,
      Literal.DataType dataType,
      FieldPointer value,
      FieldPointer display) {
    List<EnumVal> enumVals = queryPossibleEnumVals(countedIdField, dataType, value, display);

    // Check that there is exactly one display per value.
    Map<Literal, String> valDisplay = new HashMap<>();
    if (enumVals != null) {
      enumVals.stream()
          .forEach(
              enumVal -> {
                if (valDisplay.containsKey(enumVal.getValueDisplay().getValue())) {
                  throw new InvalidConfigException(
                      "Found >1 possible display for the enum value "
                          + enumVal.getValueDisplay().getValue());
                } else {
                  valDisplay.put(
                      enumVal.getValueDisplay().getValue(), enumVal.getValueDisplay().getDisplay());
                }
              });
    }
    return new EnumVals(enumVals);
  }

  private static List<EnumVal> queryPossibleEnumVals(
      FieldPointer countedIdField,
      Literal.DataType dataType,
      FieldPointer value,
      @Nullable FieldPointer display) {
    List<TableVariable> nestedQueryTables = new ArrayList<>();
    TableVariable nestedPrimaryTable = TableVariable.forPrimary(value.getTablePointer());
    nestedQueryTables.add(nestedPrimaryTable);

    FieldVariable nestedValueFieldVar =
        value.buildVariable(nestedPrimaryTable, nestedQueryTables, ENUM_VALUE_COLUMN_ALIAS);
    FieldVariable nestedDisplayFieldVar = null;
    if (display != null) {
      if (!value.getTablePointer().equals(display.getTablePointer())) {
        throw new UnsupportedOperationException(
            "Value and display must be in the same table to compute enum values display hint.");
      }
      nestedDisplayFieldVar =
          display.buildVariable(nestedPrimaryTable, nestedQueryTables, ENUM_DISPLAY_COLUMN_ALIAS);
    }
    FieldPointer countFieldPointer = countedIdField.toBuilder().sqlFunctionWrapper("COUNT").build();
    FieldVariable nestedCountFieldVar =
        countFieldPointer.buildVariable(
            nestedPrimaryTable, nestedQueryTables, ENUM_COUNT_COLUMN_ALIAS);

    List<FieldVariable> selectFields =
        display == null
            ? List.of(nestedValueFieldVar, nestedCountFieldVar)
            : List.of(nestedValueFieldVar, nestedCountFieldVar, nestedDisplayFieldVar);
    List<FieldVariable> groupByFields =
        display == null
            ? List.of(nestedValueFieldVar)
            : List.of(nestedValueFieldVar, nestedDisplayFieldVar);

    Query query =
        new Query.Builder()
            .select(selectFields)
            .tables(nestedQueryTables)
            .orderBy(List.of(new OrderByVariable(nestedValueFieldVar)))
            .groupBy(groupByFields)
            .build();
    List<ColumnSchema> columnSchemas = new ArrayList<>();
    columnSchemas.add(
        new ColumnSchema(
            ENUM_VALUE_COLUMN_ALIAS, CellValue.SQLDataType.fromUnderlayDataType(dataType)));
    columnSchemas.add(new ColumnSchema(ENUM_COUNT_COLUMN_ALIAS, CellValue.SQLDataType.INT64));
    if (display != null) {
      columnSchemas.add(new ColumnSchema(ENUM_DISPLAY_COLUMN_ALIAS, CellValue.SQLDataType.STRING));
    }

    DataPointer dataPointer = value.getTablePointer().getDataPointer();
    QueryRequest queryRequest =
        new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    // iterate through the query results, building the list of enum values
    List<EnumVal> enumVals = new ArrayList<>();
    Iterator<RowResult> rowResultIter = queryResult.getRowResults().iterator();
    while (rowResultIter.hasNext()) {
      RowResult rowResult = rowResultIter.next();
      CellValue cellValue = rowResult.get(ENUM_VALUE_COLUMN_ALIAS);
      ValueDisplay valueDisplay =
          (display == null)
              ? new ValueDisplay(cellValue.getLiteral().orElse(new Literal((String) null)))
              : new ValueDisplay(
                  cellValue.getLiteral().orElse(new Literal((String) null)),
                  rowResult.get(ENUM_DISPLAY_COLUMN_ALIAS).getString().orElse(null));
      enumVals.add(
          new EnumVal(valueDisplay, rowResult.get(ENUM_COUNT_COLUMN_ALIAS).getLong().getAsLong()));
      if (enumVals.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
        // if there are more than the max number of values, then skip the display hint
        LOGGER.info(
            "Skipping enum values display hint because there are >{} possible values: {}",
            MAX_ENUM_VALS_FOR_DISPLAY_HINT,
            value.getColumnName());
        return null;
      }
    }
    return enumVals;
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
