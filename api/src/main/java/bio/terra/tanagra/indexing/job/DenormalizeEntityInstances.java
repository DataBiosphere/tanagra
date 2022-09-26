package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.EntityJob;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.TablePointer;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DenormalizeEntityInstances extends EntityJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(DenormalizeEntityInstances.class);

  public DenormalizeEntityInstances(Entity entity) {
    super(entity);
  }

  @Override
  public void dryRun() {
    run(true);
  }

  @Override
  public void run() {
    run(false);
  }

  private void run(boolean isDryRun) {
    Query selectAllAttributes =
        getEntity().getSourceDataMapping().queryAttributes(getEntity().getAttributes());
    String sql = selectAllAttributes.renderSQL();
    LOGGER.info("select all attributes SQL: {}", sql);

    BigQueryDataset outputBQDataset = getOutputDataPointer();
    TablePointer outputTable = getEntity().getIndexDataMapping().getTablePointer();
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

  @Override
  public JobStatus checkStatus() {
    // Check if the table already exists. We don't expect this to be a long-running operation, so
    // there is no IN_PROGRESS state for this job.
    BigQueryDataset outputBQDataset = getOutputDataPointer();
    TablePointer outputTable = getEntity().getIndexDataMapping().getTablePointer();
    GoogleBigQuery googleBigQuery = outputBQDataset.getBigQueryService();
    Optional<Table> tableOpt =
        googleBigQuery.getTable(
            outputBQDataset.getProjectId(),
            outputBQDataset.getDatasetId(),
            outputTable.getTableName());
    return tableOpt.isPresent() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  private BigQueryDataset getOutputDataPointer() {
    TablePointer outputTable = getEntity().getIndexDataMapping().getTablePointer();
    DataPointer outputDataPointer = outputTable.getDataPointer();
    if (!(outputDataPointer instanceof BigQueryDataset)) {
      throw new InvalidConfigException(
          "DenormalizeEntityInstances indexing job only supports BigQuery");
    }
    return (BigQueryDataset) outputDataPointer;
  }
}
