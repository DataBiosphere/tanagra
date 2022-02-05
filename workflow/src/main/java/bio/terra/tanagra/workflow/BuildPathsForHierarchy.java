package bio.terra.tanagra.workflow;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
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
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;
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

    @Description("What to name the numChildren column on the output BigQuery table.")
    @Default.String("numChildren")
    String getOutputNumChildrenColumn();

    void setOutputNumChildrenColumn(String numChildrenColumn);
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

    // read in the nodes and the child-parent relationships from BQ
    PCollection<Long> allNodesPC = readAllNodesFromBQ(pipeline, allNodesQuery);
    PCollection<KV<Long, Long>> childParentRelationshipsPC =
        readChildParentRelationshipsFromBQ(pipeline, hierarchyQuery);

    // compute a path to a root node for each node in the hierarchy
    PCollection<KV<Long, String>> nodePathKVsPC =
        PathUtils.computePaths(
            allNodesPC, childParentRelationshipsPC, options.getMaxHierarchyDepth());

    // count the number of children for each node in the hierarchy
    PCollection<KV<Long, Long>> nodeNumChildrenKVsPC =
        PathUtils.countChildren(allNodesPC, childParentRelationshipsPC);

    // prune orphan nodes from the hierarchy (i.e. set path=null for nodes with no parents or
    // children)
    PCollection<KV<Long, String>> nodePrunedPathKVsPC =
        PathUtils.pruneOrphanPaths(nodePathKVsPC, nodeNumChildrenKVsPC);

    // write the node-{path, numChildren} pairs to BQ
    writeNodePathAndNumChildrenToBQ(
        nodePrunedPathKVsPC,
        nodeNumChildrenKVsPC,
        options.getOutputBigQueryTable(),
        pathsForHierarchySchema(options));

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
   * Read all the child-parent relationships from BQ and build a {@link PCollection} of {@link KV}
   * pairs (child, parent).
   */
  private static PCollection<KV<Long, Long>> readChildParentRelationshipsFromBQ(
      Pipeline pipeline, String sqlQuery) {
    PCollection<TableRow> childParentBqRows =
        pipeline.apply(
            "read all (child, parent) rows",
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return childParentBqRows.apply(
        "build (child, parent) pcollection",
        MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
            .via(
                tableRow -> {
                  Long parent = Long.parseLong((String) tableRow.get("parent"));
                  Long child = Long.parseLong((String) tableRow.get("child"));
                  return KV.of(child, parent);
                }));
  }

  /** Write the {@link KV} pairs (node, path) to BQ. */
  private static void writeNodePathAndNumChildrenToBQ(
      PCollection<KV<Long, String>> nodePathKVs,
      PCollection<KV<Long, Long>> nodeNumChildrenKVs,
      String outputBQTable,
      TableSchema outputBQSchema) {
    // define the CoGroupByKey tags
    final TupleTag<String> pathTag = new TupleTag<>();
    final TupleTag<Long> numChildrenTag = new TupleTag<>();

    // do a CoGroupByKey join of the current node-numChildren collection and the parent-child
    // collection
    PCollection<KV<Long, CoGbkResult>> pathNumChildrenJoin =
        KeyedPCollectionTuple.of(pathTag, nodePathKVs)
            .and(numChildrenTag, nodeNumChildrenKVs)
            .apply(
                "join node-path and node-numChildren collections for BQ row generation",
                CoGroupByKey.create());

    // run a ParDo for each row of the join result
    PCollection<TableRow> nodePathAndNumChildrenBQRows =
        pathNumChildrenJoin.apply(
            "run ParDo for each row of the node-path and node-numChildren join result to build the BQ (node, path, numChildren) row objects",
            ParDo.of(
                new DoFn<KV<Long, CoGbkResult>, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    KV<Long, CoGbkResult> element = context.element();
                    Long node = element.getKey();
                    Iterator<String> pathTagIter = element.getValue().getAll(pathTag).iterator();
                    Iterator<Long> numChildrenTagIter =
                        element.getValue().getAll(numChildrenTag).iterator();

                    String path = pathTagIter.next();
                    Long numChildren = numChildrenTagIter.next();

                    BuildPathsForHierarchyOptions contextOptions =
                        context.getPipelineOptions().as(BuildPathsForHierarchyOptions.class);
                    context.output(
                        new TableRow()
                            .set(contextOptions.getOutputNodeColumn(), node)
                            .set(contextOptions.getOutputPathColumn(), path)
                            .set(contextOptions.getOutputNumChildrenColumn(), numChildren));
                  }
                }));

    nodePathAndNumChildrenBQRows.apply(
        "insert the (node, path, numChildren) rows into BQ",
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
                    .setMode("NULLABLE"),
                new TableFieldSchema()
                    .setName(options.getOutputNumChildrenColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED")));
  }
}
