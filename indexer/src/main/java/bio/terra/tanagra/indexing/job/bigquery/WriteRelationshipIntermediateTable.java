package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.underlay2.indextable.ITRelationshipIdPairs;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import bio.terra.tanagra.underlay2.sourcetable.STRelationshipIdPairs;
import com.google.cloud.bigquery.TableId;
import java.util.Map;
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
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    Query sourceIdPairsQuery =
        sourceTable.getQueryAll(
            Map.of(
                sourceTable.getEntityAIdColumnSchema(),
                    ITRelationshipIdPairs.Column.ENTITY_A_ID.getSchema().getColumnName(),
                sourceTable.getEntityBIdColumnSchema(),
                    ITRelationshipIdPairs.Column.ENTITY_B_ID.getSchema().getColumnName()));
    LOGGER.info("source relationship id-pairs query: {}", sourceIdPairsQuery.renderSQL());

    // Create a new table directly from the select query.
    TableId outputTable =
        TableId.of(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            indexTable.getTablePointer().getTableName());
    bigQueryExecutor
        .getBigQueryService()
        .createTableFromQuery(outputTable, sourceIdPairsQuery.renderSQL(), null, isDryRun);
  }
}
