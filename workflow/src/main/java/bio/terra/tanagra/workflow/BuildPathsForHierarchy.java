package bio.terra.tanagra.workflow;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;

/**
 * A batch Apache Beam pipeline for building a table that contains a path (i.e. a list of ancestors
 * in order) for each node in a hierarchy. Example row: (node,path)=(123,"456.789"), where 456 is
 * the parent of 123 and 789 is the grandparent of 123.
 */
public final class BuildPathsForHierarchy {
  private BuildPathsForHierarchy() {}

  /** Options supported by {@link BuildPathsForHierarchy}. */
  public interface BuildPathsForHierarchyOptions extends BigQueryOptions {
    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve all nodes that we need"
            + "paths for. The result of the query should have one column, (node).")
    String getAllNodesQuery();

    void setAllNodesQuery(String query);

    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve the hierarchy to be"
            + "flattened. The result of the query should have two columns, (parent, child) that"
            + "defines all of the direct relationships of the hierarchy.")
    String getHierarchyQuery();

    void setHierarchyQuery(String query);

    @Description(
        "The maximum depth of ancestors present in the hierarchy. This may be larger "
            + "than the actual max depth, but if it is smaller the resulting table will be incomplete")
    @Default.Integer(64)
    int getMaxHierarchyDepth();

    void setMaxHierarchyDepth(int val);

    @Description(
        "The  \"[project_id]:[dataset_id].[table_id]\" specification of the node-path table to create.")
    String getOutputBigQueryTable();

    void setOutputBigQueryTable(String tableRef);

    @Description("What to name the node column on the output BigQuery table.")
    @Default.String("node")
    String getOutputNodeColumn();

    void setOutputNodeColumn(String nodeColumn);

    @Description("What to name the path column on the output BigQuery table.")
    @Default.String("path")
    String getOutputPathColumn();

    void setOutputPathColumn(String pathColumn);
  }

  public static void main(String[] args) throws IOException {
    BuildPathsForHierarchyOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(BuildPathsForHierarchyOptions.class);
    Pipeline pipeline = Pipeline.create(options);

    // read in the queries from files
    String allNodesQuery = Files.readString(Path.of(options.getAllNodesQuery()));
    String hierarchyQuery = Files.readString(Path.of(options.getHierarchyQuery()));

    // read in the nodes and the parent-child relationships from BQ
    PCollection<Long> allNodesPC = readAllNodesFromBQ(pipeline, allNodesQuery);
    PCollection<KV<Long, Long>> parentChildRelationshipsPC =
        readParentChildRelationshipsFromBQ(pipeline, hierarchyQuery);

    // compute a path to a root node for each node in the hierarchy
    PCollection<KV<Long, String>> nodePathKVsPC =
        PathUtils.computePaths(
            allNodesPC, parentChildRelationshipsPC, options.getMaxHierarchyDepth());

    // write the node-path pairs to BQ
    writeNodePathsToBQ(
        nodePathKVsPC, options.getOutputBigQueryTable(), pathsForHierarchySchema(options));

    pipeline.run().waitUntilFinish();
  }

  /** Read all the nodes from BQ and build a {@link PCollection} of just the node identifiers. */
  private static PCollection<Long> readAllNodesFromBQ(Pipeline pipeline, String sqlQuery) {
    PCollection<TableRow> allNodesBqRows =
        pipeline.apply(
            "read all (node) rows",
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return allNodesBqRows.apply(
        "build (node) pcollection",
        MapElements.into(TypeDescriptors.longs())
            .via(tableRow -> Long.parseLong((String) tableRow.get("node"))));
  }

  /**
   * Read all the parent-child relationships from BQ and build a {@link PCollection} of {@link KV}
   * pairs (parent, child).
   */
  private static PCollection<KV<Long, Long>> readParentChildRelationshipsFromBQ(
      Pipeline pipeline, String sqlQuery) {
    PCollection<TableRow> parentChildBqRows =
        pipeline.apply(
            "read all (parent, child) rows",
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return parentChildBqRows.apply(
        "build (parent, child) pcollection",
        MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
            .via(
                tableRow -> {
                  Long parent = Long.parseLong((String) tableRow.get("parent"));
                  Long child = Long.parseLong((String) tableRow.get("child"));
                  return KV.of(child, parent);
                }));
  }

  /** Write the {@link KV} pairs (node, path) to BQ. */
  private static void writeNodePathsToBQ(
      PCollection<KV<Long, String>> nodePathKVs, String outputBQTable, TableSchema outputBQSchema) {
    PCollection<TableRow> nodePathBQRows =
        nodePathKVs.apply(
            "build the BQ (node, path) row objects",
            ParDo.of(
                new DoFn<KV<Long, String>, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    BuildPathsForHierarchyOptions contextOptions =
                        context.getPipelineOptions().as(BuildPathsForHierarchyOptions.class);
                    KV<Long, String> kvPair = context.element();

                    context.output(
                        new TableRow()
                            .set(contextOptions.getOutputNodeColumn(), kvPair.getKey())
                            .set(
                                contextOptions.getOutputPathColumn(),
                                kvPair.getValue().isEmpty() ? null : kvPair.getValue()));
                  }
                }));
    nodePathBQRows.apply(
        "insert the (node, path) rows into BQ",
        BigQueryIO.writeTableRows()
            .to(outputBQTable)
            .withSchema(outputBQSchema)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  /** Defines the BigQuery schema for the output (node, path) table. */
  private static TableSchema pathsForHierarchySchema(BuildPathsForHierarchyOptions options) {
    return new TableSchema()
        .setFields(
            List.of(
                new TableFieldSchema()
                    .setName(options.getOutputNodeColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED"),
                // TODO: consider how to handle other node types besides integer. one possibility is
                // to serialize a list into a string. another possibility is to make the path column
                // an INTEGER/REPEATED field instead of a string, although that may only work for BQ
                // and not other backend.
                new TableFieldSchema()
                    .setName(options.getOutputPathColumn())
                    .setType("STRING")
                    .setMode("NULLABLE")));
  }
}
