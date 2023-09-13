package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.api.schema.EntityLevelDisplayHints;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.bigquery.BigQuerySchemaUtils;
import bio.terra.tanagra.underlay.*;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import bio.terra.tanagra.underlay.displayhint.EnumVals;
import bio.terra.tanagra.underlay.displayhint.NumericRange;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableId;
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

    bigQuery.createTableFromSchema(destinationTable, Schema.of(fieldList), isDryRun);

    // Sleep to make sure the table is found by the time we do the insert.
    // TODO: Change this to poll for existence instead.
    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException intEx) {
      throw new SystemException(
          "Interrupted during sleep after creating entity-level hints table", intEx);
    }

    // TODO: Validate queries for computing display hints when the dry run flag is set.
    if (isDryRun) {
      return;
    }

    // Calculate a display hint for each attribute. Build a list of all the hints as JSON records.
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

                JSONObject hintRow = new JSONObject();
                hintRow.put(
                    EntityLevelDisplayHints.Columns.ATTRIBUTE_NAME.getSchema().getColumnName(),
                    attribute.getName());
                hintRow.put(
                    EntityLevelDisplayHints.Columns.MIN.getSchema().getColumnName(),
                    range.getMinVal());
                hintRow.put(
                    EntityLevelDisplayHints.Columns.MAX.getSchema().getColumnName(),
                    range.getMaxVal());
                hintRow.put(
                    EntityLevelDisplayHints.Columns.ENUM_VALUE.getSchema().getColumnName(),
                    JSONObject.NULL);
                hintRow.put(
                    EntityLevelDisplayHints.Columns.ENUM_DISPLAY.getSchema().getColumnName(),
                    JSONObject.NULL);
                hintRow.put(
                    EntityLevelDisplayHints.Columns.ENUM_COUNT.getSchema().getColumnName(),
                    JSONObject.NULL);
                hintRecords.add(hintRow);
                LOGGER.info("hint record (numeric range): {}", hintRow);
              } else {
                ((EnumVals) hint)
                    .getEnumValsList().stream()
                        .forEach(
                            ev -> {
                              JSONObject hintRow = new JSONObject();
                              hintRow.put(
                                  EntityLevelDisplayHints.Columns.ATTRIBUTE_NAME
                                      .getSchema()
                                      .getColumnName(),
                                  attribute.getName());
                              hintRow.put(
                                  EntityLevelDisplayHints.Columns.MIN.getSchema().getColumnName(),
                                  JSONObject.NULL);
                              hintRow.put(
                                  EntityLevelDisplayHints.Columns.MAX.getSchema().getColumnName(),
                                  JSONObject.NULL);
                              hintRow.put(
                                  EntityLevelDisplayHints.Columns.ENUM_VALUE
                                      .getSchema()
                                      .getColumnName(),
                                  ev.getValueDisplay().getValue().getInt64Val());
                              hintRow.put(
                                  EntityLevelDisplayHints.Columns.ENUM_DISPLAY
                                      .getSchema()
                                      .getColumnName(),
                                  ev.getValueDisplay().getDisplay());
                              hintRow.put(
                                  EntityLevelDisplayHints.Columns.ENUM_COUNT
                                      .getSchema()
                                      .getColumnName(),
                                  ev.getCount());
                              hintRecords.add(hintRow);
                              LOGGER.info("hint record (enum val): {}", hintRow);
                            });
              }
            });

    // Do a single batch insert to BQ for all the hint rows.
    outputBQDataset
        .getBigQueryService()
        .insertWithStorageWriteApi(
            destinationTable.getProject(),
            destinationTable.getDataset(),
            destinationTable.getTable(),
            hintRecords);
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
