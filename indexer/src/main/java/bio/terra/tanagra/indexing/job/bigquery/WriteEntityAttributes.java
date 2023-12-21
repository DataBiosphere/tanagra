package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STEntityAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteEntityAttributes extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteEntityAttributes.class);

  private final STEntityAttributes sourceTable;
  private final ITEntityMain indexTable;

  public WriteEntityAttributes(
      SZIndexer indexerConfig, STEntityAttributes sourceTable, ITEntityMain indexTable) {
    super(indexerConfig);
    this.sourceTable = sourceTable;
    this.indexTable = indexTable;
  }

  @Override
  public String getEntity() {
    return sourceTable.getEntity();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return outputTableHasAtLeastOneRow() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    List<String> selectColumns = new ArrayList<>();
    List<String> insertColumns = new ArrayList<>();
    sourceTable.getAttributeValueColumnSchemas().entrySet().stream()
        .forEach(
            entry -> {
              String attributeName = entry.getKey();
              ColumnSchema attributeValueColumnSchema = entry.getValue();
              String indexColumn = indexTable.getAttributeValueField(attributeName).getColumnName();
              LOGGER.info(
                  "attribute value {}, column name {}",
                  attributeName,
                  attributeValueColumnSchema.getColumnName());
              selectColumns.add(attributeValueColumnSchema.getColumnName());
              insertColumns.add(indexColumn);
            });
    sourceTable.getAttributeDisplayColumnSchemas().entrySet().stream()
        .forEach(
            entry -> {
              String attributeName = entry.getKey();
              ColumnSchema attributeDisplayColumnSchema = entry.getValue();
              String indexColumn =
                  indexTable.getAttributeDisplayField(attributeName).getColumnName();
              LOGGER.info(
                  "attribute display {}, column name {}",
                  attributeName,
                  attributeDisplayColumnSchema.getColumnName());
              selectColumns.add(attributeDisplayColumnSchema.getColumnName());
              insertColumns.add(indexColumn);
            });

    // Build the query to insert to the index table using a select from the source table.
    String insertFromSelectSql =
        "INSERT INTO "
            + indexTable.getTablePointer().renderSQL()
            + " ("
            + insertColumns.stream().collect(Collectors.joining(", "))
            + ") SELECT "
            + selectColumns.stream().collect(Collectors.joining(", "))
            + " FROM "
            + sourceTable.getTablePointer().renderSQL();
    LOGGER.info("Generated insert SQL: {}", insertFromSelectSql);

    if (getOutputTable().isPresent()) {
      googleBigQuery.runInsertUpdateQuery(insertFromSelectSql, isDryRun);
    }
  }

  @Override
  public void clean(boolean isDryRun) {
    LOGGER.info(
        "Nothing to clean. CreateEntityTable will delete the output table, which includes all the rows inserted by this job.");
  }
}
