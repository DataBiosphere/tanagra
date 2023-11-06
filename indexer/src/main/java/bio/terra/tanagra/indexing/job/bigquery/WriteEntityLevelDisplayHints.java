package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.api2.query.ValueDisplay;
import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.bigquery.BigQueryDataset;
import bio.terra.tanagra.underlay2.entitymodel.Attribute;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.indextable.ITEntityLevelDisplayHints;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableId;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteEntityLevelDisplayHints extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteEntityLevelDisplayHints.class);
  private static final int MAX_ENUM_VALS_FOR_DISPLAY_HINT = 100;

  private final Entity entity;
  private final ITEntityMain indexAttributesTable;
  private final ITEntityLevelDisplayHints indexHintsTable;

  public WriteEntityLevelDisplayHints(
      SZIndexer indexerConfig,
      Entity entity,
      ITEntityMain indexAttributesTable,
      ITEntityLevelDisplayHints indexHintsTable) {
    super(indexerConfig);
    this.entity = entity;
    this.indexAttributesTable = indexAttributesTable;
    this.indexHintsTable = indexHintsTable;
  }

  @Override
  protected String getOutputTableName() {
    return indexHintsTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Build the column schemas.
    List<Field> fields =
        indexHintsTable.getColumnSchemas().stream()
            .map(
                columnSchema ->
                    Field.newBuilder(
                            columnSchema.getColumnName(),
                            BigQueryDataset.fromSqlDataType(columnSchema.getSqlDataType()))
                        .build())
            .collect(Collectors.toList());

    // Create an empty table with this schema.
    TableId destinationTable =
        TableId.of(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName());
    bigQueryExecutor
        .getBigQueryService()
        .createTableFromSchema(destinationTable, Schema.of(fields), null, isDryRun);

    // Calculate a display hint for each attribute. Build a list of all the hints as JSON records.
    List<List<Literal>> insertRows = new ArrayList<>();
    entity.getAttributes().stream()
        .forEach(
            attribute -> {
              if (!attribute.isComputeDisplayHint()) {
                LOGGER.info("Skip computing the hint for attribute {}", attribute.getName());
                return;
              } else if (isRangeHint(attribute)) {
                Pair<Double, Double> minMax = computeRangeHint(attribute);
                List<Literal> insertRow = new ArrayList<>();
                insertRow.add(new Literal(attribute.getName()));
                insertRow.add(new Literal(minMax.getKey()));
                insertRow.add(new Literal(minMax.getValue()));
                insertRow.add(new Literal(null));
                insertRow.add(new Literal(null));
                insertRow.add(new Literal(null));
                insertRows.add(insertRow);
                LOGGER.info(
                    "Numeric range hint: {}, {}, {}",
                    attribute.getName(),
                    minMax.getKey(),
                    minMax.getValue());
              } else if (isEnumHint(attribute)) {
                List<Pair<ValueDisplay, Long>> enumCounts = computeEnumHint(attribute);
                enumCounts.stream()
                    .forEach(
                        enumCount -> {
                          List<Literal> rowOfLiterals = new ArrayList<>();
                          rowOfLiterals.add(new Literal(attribute.getName()));
                          rowOfLiterals.add(new Literal(null));
                          rowOfLiterals.add(new Literal(null));
                          rowOfLiterals.add(enumCount.getKey().getValue());
                          rowOfLiterals.add(new Literal(enumCount.getKey().getDisplay()));
                          rowOfLiterals.add(new Literal(enumCount.getValue()));
                          insertRows.add(rowOfLiterals);
                          LOGGER.info(
                              "Enum hint: {}, {}, {}, {}",
                              attribute.getName(),
                              enumCount.getKey().getValue(),
                              enumCount.getKey().getDisplay(),
                              enumCount.getValue());
                        });
              } else {
                LOGGER.info(
                    "Attribute {} data type {} not yet supported for computing hint",
                    attribute.getName(),
                    attribute.getDataType());
                return;
              }
            });

    if (insertRows.isEmpty()) {
      LOGGER.info("No display hints to insert.");
      return;
    }

    // Poll for table existence before we try to insert the hint rows. Maximum 18 x 10 sec = 3 min.
    bigQueryExecutor
        .getBigQueryService()
        .pollForTableExistenceOrThrow(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName(),
            18,
            Duration.ofSeconds(10));

    // Sleep before inserting.
    // Sometimes even though the table is found in the existence check above, it still fails the
    // insert below.
    try {
      TimeUnit.SECONDS.sleep(30);
    } catch (InterruptedException intEx) {
      throw new SystemException(
          "Interrupted during sleep after creating entity-level hints table", intEx);
    }

    // Do a single insert to BQ for all the hint rows.
    TablePointer eldhTable = indexHintsTable.getTablePointer();
    TableVariable eldhTableVar = TableVariable.forPrimary(eldhTable);
    List<TableVariable> tableVars = new ArrayList<>(List.of(eldhTableVar));

    List<FieldVariable> eldhColFieldVars = new ArrayList<>();
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(ITEntityLevelDisplayHints.Column.ATTRIBUTE_NAME.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(ITEntityLevelDisplayHints.Column.MIN.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(ITEntityLevelDisplayHints.Column.MAX.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(ITEntityLevelDisplayHints.Column.ENUM_VALUE.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(ITEntityLevelDisplayHints.Column.ENUM_DISPLAY.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(ITEntityLevelDisplayHints.Column.ENUM_COUNT.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    InsertLiterals insertQuery = new InsertLiterals(eldhTableVar, eldhColFieldVars, insertRows);
    QueryRequest queryRequest =
        new QueryRequest(
            insertQuery.renderSQL(),
            ColumnHeaderSchema.fromColumnSchemas(indexHintsTable.getColumnSchemas()));
    if (!isDryRun) {
      bigQueryExecutor.execute(queryRequest);
    }
  }

  private static boolean isEnumHint(Attribute attribute) {
    return attribute.isValueDisplay() && Literal.DataType.INT64.equals(attribute.getDataType());
  }

  private static boolean isRangeHint(Attribute attribute) {
    if (attribute.isValueDisplay()) {
      return false;
    }
    switch (attribute.getDataType()) {
      case DOUBLE:
      case INT64:
        return true;
      default:
        return false;
    }
  }

  private Pair<Double, Double> computeRangeHint(Attribute attribute) {
    // Select the attribute values from the index entity main table.
    List<TableVariable> tableVars = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(indexAttributesTable.getTablePointer());
    tableVars.add(primaryTable);

    // Build the select fields and column schemas.
    final String minValAlias = "minVal";
    final String maxValAlias = "maxVal";
    List<FieldVariable> selectFieldVars = new ArrayList<>();
    FieldPointer minVal =
        indexAttributesTable
            .getAttributeValueField(attribute.getName())
            .toBuilder()
            .sqlFunctionWrapper("MIN")
            .build();
    selectFieldVars.add(minVal.buildVariable(primaryTable, tableVars, minValAlias));
    FieldPointer maxVal =
        indexAttributesTable
            .getAttributeValueField(attribute.getName())
            .toBuilder()
            .sqlFunctionWrapper("MAX")
            .build();
    selectFieldVars.add(maxVal.buildVariable(primaryTable, tableVars, maxValAlias));
    List<ColumnSchema> columnSchemas =
        List.of(
            new ColumnSchema(
                minValAlias, CellValue.SQLDataType.fromUnderlayDataType(attribute.getDataType())),
            new ColumnSchema(
                maxValAlias, CellValue.SQLDataType.fromUnderlayDataType(attribute.getDataType())));

    // Build the query for the max/min of the attribute field.
    Query query = new Query.Builder().select(selectFieldVars).tables(tableVars).build();
    QueryResult queryResult =
        bigQueryExecutor.execute(
            new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas)));

    RowResult rowResult = queryResult.getSingleRowResult();
    return Pair.of(
        rowResult.get(minValAlias).getDouble().getAsDouble(),
        rowResult.get(maxValAlias).getDouble().getAsDouble());
  }

  private List<Pair<ValueDisplay, Long>> computeEnumHint(Attribute attribute) {
    // Select the attribute values and displays from the index entity main table.
    List<TableVariable> tableVars = new ArrayList<>();
    TableVariable primaryTable = TableVariable.forPrimary(indexAttributesTable.getTablePointer());
    tableVars.add(primaryTable);

    // Build the field variables for the enum value, display and count.
    final String enumValAlias = "enumVal";
    final String enumDisplayAlias = "enumDisplay";
    final String countValAlias = "countVal";
    FieldVariable enumValueVar =
        indexAttributesTable
            .getAttributeValueField(attribute.getName())
            .buildVariable(primaryTable, tableVars, enumValAlias);
    FieldVariable enumDisplayVar =
        indexAttributesTable
            .getAttributeDisplayField(attribute.getName())
            .buildVariable(primaryTable, tableVars, enumDisplayAlias);
    FieldPointer countVal =
        indexAttributesTable
            .getAttributeValueField(entity.getIdAttribute().getName())
            .toBuilder()
            .sqlFunctionWrapper("COUNT")
            .build();
    FieldVariable countVar = countVal.buildVariable(primaryTable, tableVars, countValAlias);

    // Build the group by, select fields and column schemas.
    List<FieldVariable> groupByFieldVars = List.of(enumValueVar, enumDisplayVar);
    List<FieldVariable> selectFieldVars = List.of(enumValueVar, enumDisplayVar, countVar);
    List<ColumnSchema> columnSchemas =
        List.of(
            new ColumnSchema(
                enumValAlias, CellValue.SQLDataType.fromUnderlayDataType(attribute.getDataType())),
            new ColumnSchema(enumDisplayAlias, CellValue.SQLDataType.STRING),
            new ColumnSchema(countValAlias, CellValue.SQLDataType.INT64));

    // Build the query for the enum val/display of the attribute field and the count.
    Query query =
        new Query.Builder()
            .select(selectFieldVars)
            .tables(tableVars)
            .groupBy(groupByFieldVars)
            .build();
    QueryResult queryResult =
        bigQueryExecutor.execute(
            new QueryRequest(query.renderSQL(), new ColumnHeaderSchema(columnSchemas)));

    // Iterate through the results, building a list of enum value, display, and counts.
    List<Pair<ValueDisplay, Long>> enumCounts = new ArrayList<>();
    Iterator<RowResult> rowResultIter = queryResult.getRowResults().iterator();
    while (rowResultIter.hasNext()) {
      RowResult rowResult = rowResultIter.next();
      ValueDisplay enumValDisplay =
          new ValueDisplay(
              rowResult.get(enumValAlias).getLiteral().get(),
              rowResult.get(enumDisplayAlias).getString().get());
      long count = rowResult.get(countValAlias).getLong().getAsLong();
      enumCounts.add(Pair.of(enumValDisplay, count));

      if (enumCounts.size() > MAX_ENUM_VALS_FOR_DISPLAY_HINT) {
        // if there are more than the max number of values, then skip the display hint
        LOGGER.info(
            "Skipping enum values display hint because there are >{} possible values: {}",
            MAX_ENUM_VALS_FOR_DISPLAY_HINT,
            attribute.getName());
        return List.of();
      }
    }

    // Check that there is exactly one display per value.
    Map<Literal, String> valDisplay = new HashMap<>();
    enumCounts.stream()
        .forEach(
            enumCount -> {
              if (valDisplay.containsKey(enumCount.getKey().getValue())) {
                throw new InvalidConfigException(
                    "Found >1 possible display for the enum value "
                        + enumCount.getKey().getValue());
              } else {
                valDisplay.put(enumCount.getKey().getValue(), enumCount.getKey().getDisplay());
              }
            });
    return enumCounts;
  }
}
