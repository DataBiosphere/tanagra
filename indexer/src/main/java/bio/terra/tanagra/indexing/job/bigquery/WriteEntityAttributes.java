package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.*;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import bio.terra.tanagra.underlay2.sourcetable.STEntityAttributes;
import java.util.*;
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
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return outputTableHasAtLeastOneRow() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    Map<ColumnSchema, String> selectColumnAliases = new HashMap<>();
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
              selectColumnAliases.put(attributeValueColumnSchema, null);
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
              selectColumnAliases.put(attributeDisplayColumnSchema, null);
              insertColumns.add(indexColumn);
            });
    Query selectAttributesFromSourceTable = sourceTable.getQueryAll(selectColumnAliases);
    LOGGER.info("Generated select SQL: {}", selectAttributesFromSourceTable.renderSQL());

    // Build the outer insert query into the index table.
    TableVariable outputTableVar = TableVariable.forPrimary(indexTable.getTablePointer());
    InsertFromSelect insertQuery =
        new InsertFromSelect(outputTableVar, insertColumns, selectAttributesFromSourceTable);
    LOGGER.info("Generated insert SQL: {}", insertQuery.renderSQL());

    if (getOutputTable().isPresent()) {
      bigQueryExecutor.getBigQueryService().runInsertUpdateQuery(insertQuery.renderSQL(), isDryRun);
    }
  }

  @Override
  public void clean(boolean isDryRun) {
    LOGGER.info(
        "Nothing to clean. CreateEntityTable will delete the output table, which includes all the rows inserted by this job.");
  }
}
