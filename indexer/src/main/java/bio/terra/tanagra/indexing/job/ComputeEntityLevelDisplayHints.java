package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.api.schema.EntityLevelDisplayHints;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.query.bigquery.BigQuerySchemaUtils;
import bio.terra.tanagra.underlay.*;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.cloud.bigquery.Clustering;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComputeEntityLevelDisplayHints extends BigQueryIndexingJob {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ComputeEntityLevelDisplayHints.class);

  public ComputeEntityLevelDisplayHints(Entity entity) {
    super(entity);
  }

  @Override
  public String getName() {
    return "COMPUTE ENTITY-LEVEL DISPLAY HINTS (" + getEntity().getName() + ")";
  }

  @Override
  public void run(boolean isDryRun) {
    // Create an empty table with this schema.
    BigQueryDataset outputBQDataset = getBQDataPointer(getEntityIndexTable());
    TableId destinationTable =
        TableId.of(
            outputBQDataset.getProjectId(),
            outputBQDataset.getDatasetId(),
            getAuxiliaryTable().getTableName());
    GoogleBigQuery bigQuery = outputBQDataset.getBigQueryService();

    // Convert the internal representation of the table schema to the BQ object.
    List<Field> fieldList =
        BigQuerySchemaUtils.getBigQueryFieldList(EntityLevelDisplayHints.getColumns());

    bigQuery.createTableFromSchema(
        destinationTable,
        Schema.of(fieldList),
        Clustering.newBuilder()
            .setFields(
                List.of(
                    EntityLevelDisplayHints.Columns.ATTRIBUTE_NAME.getSchema().getColumnName(),
                    EntityLevelDisplayHints.Columns.ENUM_VALUE.getSchema().getColumnName()))
            .build(),
        isDryRun);

    // TODO: Validate queries for computing display hints when the dry run flag is set.
    if (isDryRun) {
      return;
    }

    // Calculate a display hint for each attribute. Build a list of all the hints as JSON records.
    List<List<Literal>> rowsOfLiterals = new ArrayList<>();
    List<JSONObject> hintRecords = new ArrayList<>();
    getEntity().getAttributes().stream()
        .forEach(
            attribute -> {
              if (getEntity().isIdAttribute(attribute) || attribute.skipCalculateDisplayHint()) {
                return;
              }
              DisplayHint hint =
                  attribute.getMapping(Underlay.MappingType.SOURCE).computeDisplayHint();
              if (hint == null) {
                return;
              }

              if (DisplayHint.Type.RANGE.equals(hint.getType())) {
                NumericRange range = (NumericRange) hint;

                List<Literal> rowOfLiterals = new ArrayList<>();
                rowOfLiterals.add(new Literal(attribute.getName()));
                rowOfLiterals.add(new Literal(range.getMinVal()));
                rowOfLiterals.add(new Literal(range.getMaxVal()));
                rowOfLiterals.add(new Literal(null));
                rowOfLiterals.add(new Literal(null));
                rowOfLiterals.add(new Literal(null));
                rowsOfLiterals.add(rowOfLiterals);
                LOGGER.info(
                    "hint record (numeric range): {}, {}, {}",
                    attribute.getName(),
                    range.getMinVal(),
                    range.getMaxVal());
              } else {
                ((EnumVals) hint)
                    .getEnumValsList().stream()
                        .forEach(
                            ev -> {
                              List<Literal> rowOfLiterals = new ArrayList<>();
                              rowOfLiterals.add(new Literal(attribute.getName()));
                              rowOfLiterals.add(new Literal(null));
                              rowOfLiterals.add(new Literal(null));
                              rowOfLiterals.add(ev.getValueDisplay().getValue());
                              rowOfLiterals.add(new Literal(ev.getValueDisplay().getDisplay()));
                              rowOfLiterals.add(new Literal(ev.getCount()));
                              rowsOfLiterals.add(rowOfLiterals);
                              LOGGER.info(
                                  "hint record (enum val): {}, {}, {}, {}",
                                  attribute.getName(),
                                  ev.getValueDisplay().getValue(),
                                  ev.getValueDisplay().getDisplay(),
                                  ev.getCount());
                            });
              }
            });

    // Poll for table existence before we try to insert the hint rows. Maximum 18 x 10 sec = 3 min.
    pollForTableExistenceOrThrow(getAuxiliaryTable(), 18, Duration.ofSeconds(10));

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
    TablePointer eldhTable = getAuxiliaryTable();
    TableVariable eldhTableVar = TableVariable.forPrimary(eldhTable);
    List<TableVariable> tableVars = new ArrayList<>(List.of(eldhTableVar));

    List<FieldVariable> eldhColFieldVars = new ArrayList<>();
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(EntityLevelDisplayHints.Columns.ATTRIBUTE_NAME.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(EntityLevelDisplayHints.Columns.MIN.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(EntityLevelDisplayHints.Columns.MAX.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(EntityLevelDisplayHints.Columns.ENUM_VALUE.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(EntityLevelDisplayHints.Columns.ENUM_DISPLAY.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    eldhColFieldVars.add(
        new FieldPointer.Builder()
            .columnName(EntityLevelDisplayHints.Columns.ENUM_COUNT.getSchema().getColumnName())
            .tablePointer(eldhTable)
            .build()
            .buildVariable(eldhTableVar, tableVars));
    InsertLiterals insertQuery = new InsertLiterals(eldhTableVar, eldhColFieldVars, rowsOfLiterals);
    QueryRequest queryRequest =
        new QueryRequest(
            insertQuery.renderSQL(),
            ColumnHeaderSchema.fromColumnSchemas(EntityLevelDisplayHints.getColumns()));
    outputBQDataset.getQueryExecutor().execute(queryRequest);
  }

  @Override
  public void clean(boolean isDryRun) {
    if (checkTableExists(getAuxiliaryTable())) {
      deleteTable(getAuxiliaryTable(), isDryRun);
    }
  }

  @Override
  public JobStatus checkStatus() {
    // Check if the table already exists.
    // We can't include a check for a single row here (e.g. checkOneRowExists(getAuxiliaryTable())),
    // because there are some cases where an entity has no display hints.
    return checkTableExists(getAuxiliaryTable()) ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  public TablePointer getAuxiliaryTable() {
    return getEntity().getMapping(Underlay.MappingType.INDEX).getDisplayHintTablePointer();
  }
}
