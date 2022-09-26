package bio.terra.tanagra.indexing;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.TablePointer;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import java.util.Optional;

public abstract class EntityJob implements IndexingJob {
  private final Entity entity;

  protected EntityJob(Entity entity) {
    this.entity = entity;
  }

  @Override
  public boolean prerequisitesComplete() {
    // Entity jobs have no prerequisites.
    return true;
  }

  public Entity getEntity() {
    return entity;
  }

  protected BigQueryDataset getOutputDataPointer() {
    TablePointer outputTable = getEntity().getIndexDataMapping().getTablePointer();
    DataPointer outputDataPointer = outputTable.getDataPointer();
    if (!(outputDataPointer instanceof BigQueryDataset)) {
      throw new InvalidConfigException("Entity indexing job only supports BigQuery");
    }
    return (BigQueryDataset) outputDataPointer;
  }

  protected JobStatus checkTableExistenceForJobStatus(TablePointer outputTable) {
    // Check if the table already exists. We don't expect this to be a long-running operation, so
    // there is no IN_PROGRESS state for this job.
    BigQueryDataset outputBQDataset = getOutputDataPointer();
    GoogleBigQuery googleBigQuery = outputBQDataset.getBigQueryService();
    Optional<Table> tableOpt =
        googleBigQuery.getTable(
            outputBQDataset.getProjectId(),
            outputBQDataset.getDatasetId(),
            outputTable.getTableName());
    return tableOpt.isPresent() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  protected void createTableFromSql(TablePointer outputTable, String sql, boolean isDryRun) {
    BigQueryDataset outputBQDataset = getOutputDataPointer();
    LOGGER.info(
        "output BQ table: project={}, dataset={}, table={}",
        outputBQDataset.getProjectId(),
        outputBQDataset.getDatasetId(),
        outputTable.getTableName());

    TableId destinationTable =
        TableId.of(
            outputBQDataset.getProjectId(),
            outputBQDataset.getDatasetId(),
            outputTable.getTableName());
    outputBQDataset.getBigQueryService().createTableFromQuery(destinationTable, sql, isDryRun);
  }
}
