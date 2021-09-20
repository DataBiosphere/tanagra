package bio.terra.tanagra.workflow;

import avro.shaded.com.google.common.collect.Lists;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.TypedRead.Method;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Distinct;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;

/**
 * A batch Apache Beam pipeline for flattening hierarchical parent-child relationships to
 * ancestor-descendant relationships.
 */
// TODO consider how to make this easier to reuse by deployers.
public final class FlattenHierarchy {
  private FlattenHierarchy() {}

  /** Options supported by {@link FlattenHierarchy}. */
  public interface FlattenHierarchyOptions extends BigQueryOptions {
    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve the hierarchy to be"
            + "flattened. The result of the query should have two columns, (parent, child) that"
            + "defines all of the direct relationships of the hierarchy to be flattened.")
    String getHierarchyQuery();

    void setHierarchyQuery(String query);

    @Description(
        "The maximum depth of ancestors present than the hierarchy. This may be larger "
            + "than the actual max depth, but if it is smaller the resulting table will be incomplete")
    @Default.Integer(64)
    int getMaxHierarchyDepth();

    void setMaxHierarchyDepth(int val);

    @Description(
        "The  \"[project_id]:[dataset_id].[table_id]\" specification of the ancestor table to create.")
    String getOutputBigQueryTable();

    void setOutputBigQueryTable(String tableRef);

    @Description("What to name the ancestor column on the output BigQuery table.")
    @Default.String("ancestor")
    String getOutputAncestorColumn();

    void setOutputAncestorColumn(String ancestorColumn);

    @Description("What to name the descendant column on the output BigQuery table.")
    @Default.String("descendant")
    String getOutputDescendantColumn();

    void setOutputDescendantColumn(String descendantColumn);
  }

  /** Parses a {@link TableRow} as a row contianing a (parent_id, child_id) long pair. */
  // TODO support string ids in addition to longs.
  private static KV<Long, Long> relationshipRowToKV(TableRow row) {
    Long parentId = Long.parseLong((String) row.get("parent"));
    Long childId = Long.parseLong((String) row.get("child"));
    return KV.of(parentId, childId);
  }

  /** Defines the BigQuery schema for the output hierarchy table. */
  private static TableSchema flattenedHierarchySchema(FlattenHierarchyOptions options) {
    return new TableSchema()
        .setFields(
            List.of(
                new TableFieldSchema()
                    .setName(options.getOutputAncestorColumn())
                    .setType("INTEGER"),
                new TableFieldSchema()
                    .setName(options.getOutputDescendantColumn())
                    .setMode("REPEATED")
                    .setType("INTEGER")));
  }

  public static void main(String[] args) throws IOException {
    FlattenHierarchyOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(FlattenHierarchyOptions.class);

    String hierarchyQuery = Files.readString(Path.of(options.getHierarchyQuery()));
    Pipeline pipeline = Pipeline.create(options);

    PCollection<KV<Long, Long>> relationships =
        pipeline
            .apply(
                "read hierarchy query",
                BigQueryIO.readTableRows()
                    .fromQuery(hierarchyQuery)
                    .withMethod(Method.EXPORT)
                    .usingStandardSql())
            .apply(
                MapElements.into(
                        TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
                    .via(FlattenHierarchy::relationshipRowToKV));
    PCollection<KV<Long, Long>> flattenedRelationships =
        GraphUtils.transitiveClosure(relationships, options.getMaxHierarchyDepth())
            .apply(Distinct.create()); // There may be duplicate descendants.
    flattenedRelationships
        .apply(GroupByKey.create())
        .apply(
            ParDo.of(
                new DoFn<KV<Long, Iterable<Long>>, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    Long ancestor = context.element().getKey();
                    List<Long> descendants = Lists.newArrayList(context.element().getValue());
                    TableRow row =
                        new TableRow()
                            .set(
                                context
                                    .getPipelineOptions()
                                    .as(FlattenHierarchyOptions.class)
                                    .getOutputAncestorColumn(),
                                ancestor)
                            .set(
                                context
                                    .getPipelineOptions()
                                    .as(FlattenHierarchyOptions.class)
                                    .getOutputDescendantColumn(),
                                descendants);
                    context.output(row);
                  }
                }))
        .apply(
            BigQueryIO.writeTableRows()
                .to(options.getOutputBigQueryTable())
                .withSchema(flattenedHierarchySchema(options))
                .withCreateDisposition(CreateDisposition.CREATE_IF_NEEDED)
                .withWriteDisposition(WriteDisposition.WRITE_EMPTY)
                .withMethod(Write.Method.FILE_LOADS));
    pipeline.run().waitUntilFinish();
  }
}
