package bio.terra.tanagra.indexing.job.bigquery;

import static bio.terra.tanagra.utils.GoogleBigQuery.LONG_QUERY_TIMEOUT;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STRelationshipIdPairs;
import com.google.cloud.bigquery.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteRelationshipIntermediateTable extends BigQueryJob {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(WriteRelationshipIntermediateTable.class);
  private final STRelationshipIdPairs sourceTable;
  private final ITRelationshipIdPairs indexTable;

  public WriteRelationshipIntermediateTable(
      SZIndexer indexerConfig,
      STRelationshipIdPairs sourceTable,
      ITRelationshipIdPairs indexTable) {
    super(indexerConfig);
    this.sourceTable = sourceTable;
    this.indexTable = indexTable;
  }

  @Override
  public String getEntityGroup() {
    return sourceTable.getEntityGroup();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    String sourceIdPairsSql =
        "SELECT "
            + SqlQueryField.of(
                    sourceTable.getEntityAIdField(),
                    ITRelationshipIdPairs.Column.ENTITY_A_ID.getSchema().getColumnName())
                .renderForSelect()
            + ", "
            + SqlQueryField.of(
                    sourceTable.getEntityBIdField(),
                    ITRelationshipIdPairs.Column.ENTITY_B_ID.getSchema().getColumnName())
                .renderForSelect()
            + " FROM "
            + sourceTable.getTablePointer().render();
    LOGGER.info("source relationship id-pairs query: {}", sourceIdPairsSql);

    // Create a new table directly from the select query.
    TableId outputTable =
        TableId.of(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            indexTable.getTablePointer().getTableName());
    if (isDryRun) {
      googleBigQuery.dryRunQuery(sourceIdPairsSql, null, null, null, outputTable, null);
    } else {
      googleBigQuery.runQuery(
          sourceIdPairsSql, null, null, null, outputTable, null, LONG_QUERY_TIMEOUT);
    }
  }
}
