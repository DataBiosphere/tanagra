package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import bio.terra.tanagra.query2.sql.SqlField;
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
    BQTranslator bqTranslator = new BQTranslator();
    String sourceIdPairsSql =
        "SELECT "
            + bqTranslator.selectSql(
                SqlField.of(
                    sourceTable.getEntityAIdField(),
                    ITRelationshipIdPairs.Column.ENTITY_A_ID.getSchema().getColumnName()),
                null)
            + ", "
            + bqTranslator.selectSql(
                SqlField.of(
                    sourceTable.getEntityBIdField(),
                    ITRelationshipIdPairs.Column.ENTITY_B_ID.getSchema().getColumnName()),
                null)
            + " FROM "
            + sourceTable.getTablePointer().renderSQL();
    LOGGER.info("source relationship id-pairs query: {}", sourceIdPairsSql);

    // Create a new table directly from the select query.
    TableId outputTable =
        TableId.of(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            indexTable.getTablePointer().getTableName());
    googleBigQuery.createTableFromQuery(outputTable, sourceIdPairsSql, null, isDryRun);
  }
}
