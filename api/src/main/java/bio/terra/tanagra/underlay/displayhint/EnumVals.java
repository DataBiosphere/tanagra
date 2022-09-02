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
import bio.terra.tanagra.serialization.displayhint.UFEnumVals;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.DisplayHint;
import bio.terra.tanagra.underlay.FieldPointer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EnumVals extends DisplayHint {
  private static final Logger LOGGER = LoggerFactory.getLogger(EnumVals.class);
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

  public List<ValueDisplay> getValueDisplays() {
    return Collections.unmodifiableList(valueDisplays);
  }

  public static EnumVals computeForField(FieldPointer value) {
    List<TableVariable> tables = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(value.getTablePointer());
    tables.add(primaryTable);

    final String enumValAlias = "enumVal";

    FieldVariable valueFieldVar = value.buildVariable(primaryTable, tables, enumValAlias);
    Query query =
        new Query(List.of(valueFieldVar), tables, List.of(valueFieldVar), List.of(valueFieldVar));

    List<ColumnSchema> columnSchemas =
        List.of(new ColumnSchema(enumValAlias, CellValue.SQLDataType.STRING));

    DataPointer dataPointer = value.getTablePointer().getDataPointer();
    QueryRequest queryRequest =
        new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas));
    QueryResult queryResult = dataPointer.getQueryExecutor().execute(queryRequest);

    List<String> enumStringVals = new ArrayList<>();
    Iterator<RowResult> rowResultIter = queryResult.getRowResults().iterator();
    while (rowResultIter.hasNext()) {
      enumStringVals.add(rowResultIter.next().get(enumValAlias).getString().orElse(null));
      if (enumStringVals.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
        // if there are more than the max number of values, then skip the display hint
        LOGGER.info(
            "Skipping enum values display hint because there are >100 possible values: "
                + valueFieldVar.getAlias());
        return null;
      }
    }
    return new EnumVals(
        enumStringVals.stream().map(esv -> new ValueDisplay(esv)).collect(Collectors.toList()));
  }
}
