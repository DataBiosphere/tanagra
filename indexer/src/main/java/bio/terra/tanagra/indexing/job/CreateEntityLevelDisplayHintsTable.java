package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.*;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableId;
import java.util.List;
import java.util.stream.Collectors;

public class CreateEntityLevelDisplayHintsTable extends BigQueryIndexingJob {
  public CreateEntityLevelDisplayHintsTable(Entity entity) {
    super(entity);
  }

  @Override
  public String getName() {
    return "CREATE ENTITY-LEVEL DISPLAY HINTS TABLE (" + getEntity().getName() + ")";
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
        Entity.DISPLAY_HINTS_TABLE_SCHEMA.entrySet().stream()
            .map(
                entry -> {
                  String columnName = entry.getKey();
                  LegacySQLTypeName columnDataType =
                      BigQueryDataset.fromSqlDataType(entry.getValue().getKey());
                  boolean isRequired = entry.getValue().getValue();
                  return Field.newBuilder(columnName, columnDataType)
                      .setMode(isRequired ? Field.Mode.REQUIRED : Field.Mode.NULLABLE)
                      .build();
                })
            .collect(Collectors.toList());

    bigQuery.createTableFromSchema(destinationTable, Schema.of(fieldList), isDryRun);
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
    return checkTableExists(getAuxiliaryTable()) ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  public TablePointer getAuxiliaryTable() {
    return getEntity().getMapping(Underlay.MappingType.INDEX).getDisplayHintTablePointer();
  }
}
