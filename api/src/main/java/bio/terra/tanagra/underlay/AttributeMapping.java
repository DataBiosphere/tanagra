package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnHeaderSchema;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryRequest;
import bio.terra.tanagra.query.QueryResult;
import bio.terra.tanagra.query.RowResult;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFAttributeMapping;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import bio.terra.tanagra.underlay.displayhint.ValueDisplay;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class AttributeMapping {
  private static final String DEFAULT_DISPLAY_MAPPING_PREFIX = "t_display_";
  private static final int MAX_ENUM_VALS_FOR_DISPLAY_HINT = 100;

  private final FieldPointer value;
  private final FieldPointer display;

  private AttributeMapping(FieldPointer value) {
    this.value = value;
    this.display = null;
  }

  private AttributeMapping(FieldPointer value, FieldPointer display) {
    this.value = value;
    this.display = display;
  }

  public static AttributeMapping fromSerialized(
      @Nullable UFAttributeMapping serialized, TablePointer tablePointer, Attribute attribute) {
    // if the value is defined, then deserialize it
    // otherwise generate a default attribute mapping: a column with the same name as the attribute
    FieldPointer value =
        (serialized != null && serialized.getValue() != null)
            ? FieldPointer.fromSerialized(serialized.getValue(), tablePointer)
            : new FieldPointer(tablePointer, attribute.getName());

    switch (attribute.getType()) {
      case SIMPLE:
        return new AttributeMapping(value);
      case KEY_AND_DISPLAY:
        // if the display is defined, then deserialize it
        // otherwise, generate a default attribute display mapping: a column with the same name as
        // the attribute with a prefix
        FieldPointer display =
            (serialized != null && serialized.getDisplay() != null)
                ? FieldPointer.fromSerialized(serialized.getDisplay(), tablePointer)
                : new FieldPointer(
                    tablePointer, DEFAULT_DISPLAY_MAPPING_PREFIX + attribute.getName());
        return new AttributeMapping(value, display);
      default:
        throw new IllegalArgumentException("Attribute type is not defined");
    }
  }

  public List<FieldVariable> buildFieldVariables(
      TableVariable primaryTable, List<TableVariable> tableVariables, String attributeName) {
    FieldVariable valueVariable = value.buildVariable(primaryTable, tableVariables, attributeName);
    if (!hasDisplay()) {
      return List.of(valueVariable);
    }

    FieldVariable displayVariable =
        display.buildVariable(
            primaryTable, tableVariables, DEFAULT_DISPLAY_MAPPING_PREFIX + attributeName);
    return List.of(valueVariable, displayVariable);
  }

  public DisplayHint computeNumericRangeHint(DataPointer dataPointer) {
    List<TableVariable> tables = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(value.getTablePointer());
    tables.add(primaryTable);

    final String minValAlias = "minVal";
    final String maxValAlias = "maxVal";

    List<FieldVariable> select = new ArrayList<>();
    FieldPointer minVal = new FieldPointer(value).setSqlFunctionWrapper("MIN");
    select.add(minVal.buildVariable(primaryTable, tables, minValAlias));
    FieldPointer maxVal = new FieldPointer(value).setSqlFunctionWrapper("MAX");
    select.add(maxVal.buildVariable(primaryTable, tables, maxValAlias));
    Query query = new Query(select, tables);

    List<ColumnSchema> columnSchemas =
        List.of(
            new ColumnSchema(minValAlias, CellValue.SQLDataType.INT64),
            new ColumnSchema(maxValAlias, CellValue.SQLDataType.INT64));

    QueryRequest queryRequest =
        new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);
    RowResult rowResult = queryResult.getSingleRowResult();
    return new NumericRange(
        rowResult.get(minValAlias).getLong().getAsLong(),
        rowResult.get(maxValAlias).getLong().getAsLong());
  }

  public DisplayHint computeEnumValsHint(DataPointer dataPointer) {
    List<TableVariable> tables = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(value.getTablePointer());
    tables.add(primaryTable);

    final String enumValAlias = "enumVal";

    FieldVariable valueFieldVar = value.buildVariable(primaryTable, tables, enumValAlias);
    Query query =
        new Query(List.of(valueFieldVar), tables, List.of(valueFieldVar), List.of(valueFieldVar));

    List<ColumnSchema> columnSchemas =
        List.of(new ColumnSchema(enumValAlias, CellValue.SQLDataType.STRING));

    QueryRequest queryRequest =
        new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    List<String> enumStringVals = new ArrayList<>();
    Iterator<RowResult> rowResultIter = queryResult.getRowResults().iterator();
    while (rowResultIter.hasNext()) {
      enumStringVals.add(rowResultIter.next().get(enumValAlias).getString().orElse(null));
      if (enumStringVals.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
        // if there are more than the max number of values, then skip the display hint
        return null;
      }
    }
    return new EnumVals(
        enumStringVals.stream().map(esv -> new ValueDisplay(esv)).collect(Collectors.toList()));
  }

  public boolean hasDisplay() {
    return display != null;
  }

  public FieldPointer getValue() {
    return value;
  }

  public FieldPointer getDisplay() {
    return display;
  }
}
