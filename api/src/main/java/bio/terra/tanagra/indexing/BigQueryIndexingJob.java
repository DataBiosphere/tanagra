package bio.terra.tanagra.indexing;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.EntityGroup;
import bio.terra.tanagra.underlay.TablePointer;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import java.io.IOException;
import java.util.Optional;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;

public abstract class BigQueryIndexingJob implements IndexingJob {
  protected static final String DEFAULT_REGION = "us-central1";

  // The maximum depth of ancestors present in a hierarchy. This may be larger
  // than the actual max depth, but if it is smaller the resulting table will be incomplete.
  // TODO: Allow overriding the default max hierarchy depth.
  protected static final int DEFAULT_MAX_HIERARCHY_DEPTH = 64;

  private final Entity entity;
  private final EntityGroup entityGroup;

  protected BigQueryIndexingJob(Entity entity) {
    this.entity = entity;
    this.entityGroup = null;
  }

  protected BigQueryIndexingJob(EntityGroup entityGroup) {
    this.entity = null;
    this.entityGroup = entityGroup;
  }

  @Override
  public boolean prerequisitesComplete() {
    // Entity jobs have no prerequisites.
    return true;
  }

  @Override
  public void dryRun() {
    run(true);
  }

  @Override
  public void run() {
    run(false);
  }

  protected abstract void run(boolean isDryRun);

  protected Entity getEntity() {
    return entity;
  }

  protected EntityGroup getEntityGroup() {
    return entityGroup;
  }

  protected abstract TablePointer getOutputTablePointer();

  protected BigQueryDataset getOutputDataPointer() {
    DataPointer outputDataPointer = getOutputTablePointer().getDataPointer();
    if (!(outputDataPointer instanceof BigQueryDataset)) {
      throw new InvalidConfigException("Entity indexing job only supports BigQuery");
    }
    return (BigQueryDataset) outputDataPointer;
  }

  @Override
  public JobStatus checkStatus() {
    // Check if the table already exists. We don't expect this to be a long-running operation, so
    // there is no IN_PROGRESS state for this job.
    TablePointer outputTable = getOutputTablePointer();
    BigQueryDataset outputBQDataset = getOutputDataPointer();
    LOGGER.info(
        "output BQ table: project={}, dataset={}, table={}",
        outputBQDataset.getProjectId(),
        outputBQDataset.getDatasetId(),
        outputTable.getTableName());
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
    TableId destinationTable =
        TableId.of(
            outputBQDataset.getProjectId(),
            outputBQDataset.getDatasetId(),
            outputTable.getTableName());
    outputBQDataset.getBigQueryService().createTableFromQuery(destinationTable, sql, isDryRun);
  }

  protected BigQueryOptions buildDataflowPipelineOptions(BigQueryDataset outputBQDataset) {
    // TODO: Allow overriding the default region.
    String[] args = {
      "--runner=dataflow",
      "--project=" + outputBQDataset.getProjectId(),
      "--region=" + DEFAULT_REGION,
      "--serviceAccount=" + getAppDefaultSAEmail(),
    };
    // TODO: Use PipelineOptionsFactory.create() instead of fromArgs().
    // PipelineOptionsFactory.create() doesn't seem to call the default factory classes, while
    // fromArgs() does.
    return PipelineOptionsFactory.fromArgs(args).withValidation().as(BigQueryOptions.class);
  }

  protected String getAppDefaultSAEmail() {
    try {
      GoogleCredentials appDefaultSACredentials = GoogleCredentials.getApplicationDefault();
      String serviceAccountEmail =
          ((ServiceAccountCredentials) appDefaultSACredentials).getClientEmail();
      LOGGER.info("Service account email: {}", serviceAccountEmail);
      return serviceAccountEmail;
    } catch (IOException ioEx) {
      throw new SystemException("Error reading application default credentials.", ioEx);
    }
  }
}
