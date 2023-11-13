package bio.terra.tanagra.indexing.job.dataflow;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.DataflowUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.GraphUtils;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STHierarchyChildParent;
import com.google.api.services.bigquery.model.Clustering;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.Distinct;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A batch Apache Beam pipeline for flattening hierarchical parent-child relationships to
 * ancestor-descendant relationships.
 */
public class WriteAncestorDescendant extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteAncestorDescendant.class);

  private static final String CHILD_COLUMN_NAME = "child";
  private static final String PARENT_COLUMN_NAME = "parent";
  private final Hierarchy hierarchy;
  private final STHierarchyChildParent sourceTable;
  private final ITHierarchyAncestorDescendant indexTable;

  public WriteAncestorDescendant(
      SZIndexer indexerConfig,
      Hierarchy hierarchy,
      STHierarchyChildParent sourceTable,
      ITHierarchyAncestorDescendant indexTable) {
    super(indexerConfig);
    this.hierarchy = hierarchy;
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
    // Build the pipeline object from the Dataflow config.
    Pipeline pipeline = Pipeline.create(DataflowUtils.getPipelineOptions(indexerConfig, getName()));

    // Build the source child-parent query and the pipeline steps to read the results.
    Query sourceChildParentQuery =
        sourceTable.getQueryAll(
            Map.of(
                sourceTable.getChildColumnSchema(), CHILD_COLUMN_NAME,
                sourceTable.getParentColumnSchema(), PARENT_COLUMN_NAME));
    LOGGER.info("source child-parent query: {}", sourceChildParentQuery.renderSQL());
    PCollection<KV<Long, Long>> relationships =
        pipeline
            .apply(
                "read parent-child query result",
                BigQueryIO.readTableRows()
                    .fromQuery(sourceChildParentQuery.renderSQL())
                    .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                    .usingStandardSql())
            .apply(
                MapElements.into(
                        TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
                    .via(WriteAncestorDescendant::relationshipRowToKV));

    // Build the ancestor-descendant computation steps into the pipeline.
    PCollection<KV<Long, Long>> flattenedRelationships =
        GraphUtils.transitiveClosure(relationships, hierarchy.getMaxDepth())
            .apply(Distinct.create()); // There may be duplicate descendants.

    // Build the schema for the output table.
    List<TableFieldSchema> outputFieldSchemas =
        indexTable.getColumnSchemas().stream()
            .map(
                columnSchema ->
                    new TableFieldSchema()
                        .setName(columnSchema.getColumnName())
                        .setType(
                            BigQueryBeamUtils.fromSqlDataType(columnSchema.getSqlDataType()).name())
                        .setMode(columnSchema.isRequired() ? "REQUIRED" : "NULLABLE"))
            .collect(Collectors.toList());
    TableSchema outputTableSchema = new TableSchema().setFields(outputFieldSchemas);
    Clustering clustering =
        new Clustering().setFields(List.of(indexTable.getAncestorField().getColumnName()));

    // Build the output-to-bigquery steps into the pipeline.
    flattenedRelationships
        .apply(ParDo.of(new KVToTableRow()))
        .apply(
            BigQueryIO.writeTableRows()
                .to(
                    BigQueryBeamUtils.getTableSqlPath(
                        indexerConfig.bigQuery.indexData.projectId,
                        indexerConfig.bigQuery.indexData.datasetId,
                        indexTable.getTablePointer().getTableName()))
                .withSchema(outputTableSchema)
                .withClustering(clustering)
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
                .withMethod(BigQueryIO.Write.Method.FILE_LOADS));

    // Kick off the pipeline.
    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  /** Parses a {@link TableRow} as a row containing a (parent_id, child_id) long pair. */
  private static KV<Long, Long> relationshipRowToKV(TableRow row) {
    Long parentId = Long.parseLong((String) row.get(PARENT_COLUMN_NAME));
    Long childId = Long.parseLong((String) row.get(CHILD_COLUMN_NAME));
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
              .set(
                  ITHierarchyAncestorDescendant.Column.ANCESTOR.getSchema().getColumnName(),
                  ancestor)
              .set(
                  ITHierarchyAncestorDescendant.Column.DESCENDANT.getSchema().getColumnName(),
                  descendant);
      context.output(row);
    }
  }
}
