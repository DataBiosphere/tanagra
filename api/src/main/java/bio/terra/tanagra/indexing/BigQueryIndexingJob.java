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
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.base.MoreObjects;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public abstract class BigQueryIndexingJob implements IndexingJob {
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormat.forPattern("MMddHHmm").withZone(DateTimeZone.UTC);

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
    // TODO: Implement a required ordering so we can run jobs in parallel.
    return true;
  }

  protected Entity getEntity() {
    return entity;
  }

  protected EntityGroup getEntityGroup() {
    return entityGroup;
  }

  @VisibleForTesting
  public abstract TablePointer getOutputTablePointer();

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

  @Override
  public void clean(boolean isDryRun) {
    // Delete the output table.
    BigQueryDataset outputBQDataset = getOutputDataPointer();
    if (isDryRun) {
      LOGGER.info(
          "Delete table: {}, {}, {}",
          outputBQDataset.getProjectId(),
          outputBQDataset.getDatasetId(),
          getOutputTablePointer().getTableName());
    } else {
      getOutputDataPointer()
          .getBigQueryService()
          .deleteTable(
              outputBQDataset.getProjectId(),
              outputBQDataset.getDatasetId(),
              getOutputTablePointer().getTableName());
    }
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
    // If the BQ dataset defines a service account, then specify that.
    // Otherwise, try to get the service account email for the application default credentials.
    String serviceAccountEmail = outputBQDataset.getDataflowServiceAccountEmail();
    if (serviceAccountEmail == null) {
      serviceAccountEmail = getAppDefaultSAEmail();
    }
    LOGGER.info("Dataflow service account: {}", serviceAccountEmail);

    DataflowPipelineOptions dataflowOptions =
        PipelineOptionsFactory.create().as(DataflowPipelineOptions.class);
    dataflowOptions.setRunner(DataflowRunner.class);
    dataflowOptions.setProject(outputBQDataset.getProjectId());
    // TODO: Allow overriding the default region.
    dataflowOptions.setRegion(DEFAULT_REGION);
    dataflowOptions.setServiceAccount(serviceAccountEmail);
    dataflowOptions.setJobName(getDataflowJobName());

    if (outputBQDataset.getDataflowTempLocation() != null) {
      dataflowOptions.setTempLocation(outputBQDataset.getDataflowTempLocation());
      LOGGER.info("Dataflow temp location: {}", dataflowOptions.getTempLocation());
    }

    return dataflowOptions;
  }

  /** Build a name for the Dataflow job that will be visible in the Cloud Console. */
  private String getDataflowJobName() {
    String jobDisplayName = getName();
    String normalizedJobDisplayName =
        jobDisplayName == null || jobDisplayName.length() == 0
            ? "t-BQIndexingJob"
            : "t-" + jobDisplayName.toLowerCase().replaceAll("[^a-z0-9]", "-");
    String userName = MoreObjects.firstNonNull(System.getProperty("user.name"), "");
    String normalizedUserName = userName.toLowerCase().replaceAll("[^a-z0-9]", "0");
    String datePart = FORMATTER.print(DateTimeUtils.currentTimeMillis());

    String randomPart = Integer.toHexString(ThreadLocalRandom.current().nextInt());
    return String.format(
        "%s-%s-%s-%s", normalizedJobDisplayName, normalizedUserName, datePart, randomPart);
  }

  protected String getAppDefaultSAEmail() {
    GoogleCredentials appDefaultSACredentials;
    try {
      appDefaultSACredentials = GoogleCredentials.getApplicationDefault();
    } catch (IOException ioEx) {
      throw new SystemException("Error reading application default credentials.", ioEx);
    }

    // Get email if this is a service account.
    try {
      String serviceAccountEmail =
          ((ServiceAccountCredentials) appDefaultSACredentials).getClientEmail();
      LOGGER.info("Service account email: {}", serviceAccountEmail);
      return serviceAccountEmail;
    } catch (ClassCastException ccEx) {
      LOGGER.debug("Application default credentials are not a service account.", ccEx);
    }

    return "";
  }
}
