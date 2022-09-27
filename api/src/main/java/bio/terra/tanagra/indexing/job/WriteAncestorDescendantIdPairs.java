package bio.terra.tanagra.indexing.job;

import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.EntityJob;
import bio.terra.tanagra.indexing.job.beam.GraphUtils;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.TablePointer;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import java.util.List;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Distinct;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteAncestorDescendantIdPairs extends EntityJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteParentChildIdPairs.class);

  private static final String DEFAULT_REGION = "us-central1";

  // The maximum depth of ancestors present than the hierarchy. This may be larger
  // than the actual max depth, but if it is smaller the resulting table will be incomplete.
  private static final int DEFAULT_MAX_HIERARCHY_DEPTH = 64;

  // The default table schema for the ancestor-descendant output table.
  private static final String ANCESTOR_COLUMN_NAME = "ancestor";
  private static final String DESCENDANT_COLUMN_NAME = "descendant";
  private static final TableSchema ANCESTOR_DESCENDANT_TABLE_SCHEMA =
      new TableSchema()
          .setFields(
              List.of(
                  new TableFieldSchema()
                      .setName(ANCESTOR_COLUMN_NAME)
                      .setType("INTEGER")
                      .setMode("REQUIRED"),
                  new TableFieldSchema()
                      .setName(DESCENDANT_COLUMN_NAME)
                      .setType("INTEGER")
                      .setMode("REQUIRED")));

  private final String hierarchyName;

  public WriteAncestorDescendantIdPairs(Entity entity, String hierarchyName) {
    super(entity);
    this.hierarchyName = hierarchyName;
  }

  @Override
  public String getName() {
    return "WRITE ANCESTOR-DESCENDANT ID PAIRS ("
        + getEntity().getName()
        + ", "
        + hierarchyName
        + ")";
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
    SQLExpression selectChildParentIdPairs =
        getEntity()
            .getSourceDataMapping()
            .getHierarchyMapping(hierarchyName)
            .queryChildParentPairs("child", "parent");
    String sql = selectChildParentIdPairs.renderSQL();
    LOGGER.info("select all ancestor-descendant id pairs SQL: {}", sql);

    TablePointer outputTable =
        getEntity()
            .getIndexDataMapping()
            .getHierarchyMapping(hierarchyName)
            .getAncestorDescendant()
            .getTablePointer();
    BigQueryDataset outputBQDataset = getOutputDataPointer();
    LOGGER.info(
        "output BQ table: project={}, dataset={}, table={}",
        outputBQDataset.getProjectId(),
        outputBQDataset.getDatasetId(),
        outputTable.getTableName());

    String serviceAccountEmail;
    try {
      GoogleCredentials appDefaultSACredentials = GoogleCredentials.getApplicationDefault();
      serviceAccountEmail = ((ServiceAccountCredentials) appDefaultSACredentials).getClientEmail();
      LOGGER.info("Service account email: {}", serviceAccountEmail);
    } catch (IOException ioEx) {
      throw new SystemException("Error reading application default credentials.", ioEx);
    }

    // TODO: Allow overriding the default region.
    String[] args = {
      "--runner=dataflow",
      "--project=" + outputBQDataset.getProjectId(),
      "--region=" + DEFAULT_REGION,
      "--serviceAccount=" + serviceAccountEmail,
    };
    // TODO: Use PipelineOptionsFactory.create() instead of fromArgs().
    // PipelineOptionsFactory.create() doesn't seem to call the default factory classes, while
    // fromArgs() does.
    BigQueryOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(BigQueryOptions.class);

    Pipeline pipeline = Pipeline.create(options);
    PCollection<KV<Long, Long>> relationships =
        pipeline
            .apply(
                "read parent-child query result",
                BigQueryIO.readTableRows()
                    .fromQuery(sql)
                    .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                    .usingStandardSql())
            .apply(
                MapElements.into(
                        TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
                    .via(WriteAncestorDescendantIdPairs::relationshipRowToKV));
    // TODO: Allow overriding the default max hierarchy depth.
    PCollection<KV<Long, Long>> flattenedRelationships =
        GraphUtils.transitiveClosure(relationships, DEFAULT_MAX_HIERARCHY_DEPTH)
            .apply(Distinct.create()); // There may be duplicate descendants.
    flattenedRelationships
        .apply(ParDo.of(new KVToTableRow()))
        .apply(
            BigQueryIO.writeTableRows()
                .to(outputTable.getPathForIndexing())
                .withSchema(ANCESTOR_DESCENDANT_TABLE_SCHEMA)
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
                .withMethod(BigQueryIO.Write.Method.FILE_LOADS));

    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  @Override
  public JobStatus checkStatus() {
    return checkTableExistenceForJobStatus(
        getEntity()
            .getIndexDataMapping()
            .getHierarchyMapping(hierarchyName)
            .getAncestorDescendant()
            .getTablePointer());
  }

  /** Parses a {@link TableRow} as a row containing a (parent_id, child_id) long pair. */
  private static KV<Long, Long> relationshipRowToKV(TableRow row) {
    // TODO: Support id data types other than longs.
    Long parentId = Long.parseLong((String) row.get("parent"));
    Long childId = Long.parseLong((String) row.get("child"));
    return KV.of(parentId, childId);
  }

  /**
   * Converts a KV pair to a BQ TableRow object. This DoFn is defined in a separate static class
   * instead of an anonymous inner (inline) one so that it will be Serializable. Anonymous inner
   * classes include a pointer to the containing class, which here is not Serializable.
   */
  private static class KVToTableRow extends DoFn<KV<Long, Long>, TableRow> {
    @ProcessElement
    public void processElement(ProcessContext context) {
      Long ancestor = context.element().getKey();
      Long descendant = context.element().getValue();
      TableRow row =
          new TableRow()
              .set(ANCESTOR_COLUMN_NAME, ancestor)
              .set(DESCENDANT_COLUMN_NAME, descendant);
      context.output(row);
    }
  }
}
