package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.IndexingJob;
import bio.terra.tanagra.query.bigquery.BigQueryExecutor;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import com.google.cloud.bigquery.Table;
import java.util.Optional;

public abstract class BigQueryJob implements IndexingJob {
  protected final SZIndexer indexerConfig;
  protected final BigQueryExecutor bigQueryExecutor;

  protected BigQueryJob(SZIndexer indexerConfig) {
    this.indexerConfig = indexerConfig;
    this.bigQueryExecutor =
        new BigQueryExecutor(
            indexerConfig.bigQuery.queryProjectId, indexerConfig.bigQuery.dataLocation);
  }

  @Override
  public void clean(boolean isDryRun) {
    Optional<Table> outputTable = getOutputTable();
    if (outputTable.isPresent()) {
      LOGGER.info("Deleting output table: {}", outputTable.get().getFriendlyName());
      if (!isDryRun) {
        bigQueryExecutor
            .getBigQueryService()
            .deleteTable(
                indexerConfig.bigQuery.indexData.projectId,
                indexerConfig.bigQuery.indexData.datasetId,
                getOutputTableName());
      }
    } else {
      LOGGER.info("Output table not found. Nothing to delete.");
    }
  }

  protected abstract String getOutputTableName();

  protected Optional<Table> getOutputTable() {
    return bigQueryExecutor
        .getBigQueryService()
        .getTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName());
  }

  protected long getRowsInOutputTable() {
    return bigQueryExecutor
        .getBigQueryService()
        .getNumRows(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName());
  }

  protected long getRowsWhereFieldIsNotNull(String field) {
    return bigQueryExecutor
        .getBigQueryService()
        .getNumRowsWhereFieldNotNull(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName(),
            field);
  }
}
