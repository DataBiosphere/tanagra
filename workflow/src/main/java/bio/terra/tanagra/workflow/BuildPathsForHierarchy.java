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
 * in order) for each node in a hierarchy. Example row: (node,path)=(123,"456.789"), where 456
 * is the parent of 123 and 789 is the grandparent of 123.
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

  /**
   * Convert a BQ {@link TableRow} to an Apache Beam {@link KV} pair for the (node, starting path).
   * The starting path is just the node itself.
   */
  private static KV<Long, String> bqRowToNodePathKV(TableRow row) {
    Long node = Long.parseLong((String) row.get("node"));
    return KV.of(node, node.toString());
  }

  /**
   * Convert a BQ {@link TableRow} to an Apache Beam {@link KV} pair for the (child, parent)
   * relationship.
   */
  private static KV<Long, String> bqRowToChildParentKV(TableRow row) {
    Long parent = Long.parseLong((String) row.get("parent"));
    Long child = Long.parseLong((String) row.get("child"));

    return KV.of(child, parent.toString());
  }

  /** Defines the BigQuery schema for the output hierarchy table. */
  private static TableSchema pathsForHierarchySchema(BuildPathsForHierarchyOptions options) {
    return new TableSchema()
        .setFields(
            List.of(
                new TableFieldSchema()
                    .setName(options.getOutputNodeColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED"),
                // TODO: make the path column an INTEGER/REPEATED field instead of a string so we
                // can handle other node types
                new TableFieldSchema()
                    .setName(options.getOutputPathColumn())
                    .setType("STRING")
                    .setMode("NULLABLE")));
  }

  public static void main(String[] args) throws IOException {
    BuildPathsForHierarchyOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(BuildPathsForHierarchyOptions.class);

    // read in the queries from files
    String allNodesQuery = Files.readString(Path.of(options.getAllNodesQuery()));
    String hierarchyQuery = Files.readString(Path.of(options.getHierarchyQuery()));

    Pipeline pipeline = Pipeline.create(options);

    // read in all (parent, child) rows
    PCollection<TableRow> parentChildBQrows =
        pipeline.apply(
            "read all (parent,child) rows",
            BigQueryIO.readTableRows()
                .fromQuery(hierarchyQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());

    // build a collection of KV<child,parent>
    PCollection<KV<Long, String>> childParentKVs =
        parentChildBQrows.apply(
            "build (child,parent) KV pairs",
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.strings()))
                .via(BuildPathsForHierarchy::bqRowToChildParentKV));

    // read in all (node) rows
    PCollection<TableRow> nodeBqRows =
        pipeline.apply(
            "read all (node) rows",
            BigQueryIO.readTableRows()
                .fromQuery(allNodesQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());

    // build a collection of KV<node,path> where path=node
    PCollection<KV<Long, String>> nextNodePathKVs =
        nodeBqRows.apply(
            "build (node,path) KV pairs",
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.strings()))
                .via(BuildPathsForHierarchy::bqRowToNodePathKV));

    // define the CoGroupByKey tags
    final TupleTag<String> pathTag = new TupleTag<>();
    final TupleTag<String> parentTag = new TupleTag<>();

    // iterate through each possible level of the hierarchy, adding up to one node to each path per
    // iteration
    for (int ctr = 0; ctr < options.getMaxHierarchyDepth(); ctr++) {
      // do a CoGroupByKey join of the current node-path collection and the child-parent collection
      PCollection<KV<Long, CoGbkResult>> nodeParentPathJoin =
          KeyedPCollectionTuple.of(pathTag, nextNodePathKVs)
              .and(parentTag, childParentKVs)
              .apply(CoGroupByKey.create());

      nextNodePathKVs =
          nodeParentPathJoin.apply(
              ParDo.of(
                  new DoFn<KV<Long, CoGbkResult>, KV<Long, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext context) {
                      KV<Long, CoGbkResult> element = context.element();
                      Long node = element.getKey();
                      Iterator<String> pathTagIter = element.getValue().getAll(pathTag).iterator();
                      Iterator<String> parentTagIter =
                          element.getValue().getAll(parentTag).iterator();

                      // there may be multiple possible next steps (i.e. the current node may have
                      // multiple parents). just pick the first one
                      String nextNodeInPath = parentTagIter.hasNext() ? parentTagIter.next() : null;

                      // iterate through all the paths that need this relationship to complete their
                      // next step
                      while (pathTagIter.hasNext()) {
                        String currentPath = pathTagIter.next();

                        if (nextNodeInPath == null) {
                          // if there are no relationships to complete the next step, then we've
                          // reached a root node. just keep the path as is and move on the next one
                          context.output(KV.of(node, currentPath));
                        } else {
                          // if there is a next node, then append it to the path and make it the new
                          // key
                          context.output(
                              KV.of(
                                  Long.valueOf(nextNodeInPath),
                                  currentPath + "." + nextNodeInPath));
                        }
                      }
                    }
                  }));
    }
    // example for CoGroupByKey+ParDo iteration above:
    //    desired result (node,path)
    //    (a1,"a1.a2.a3")
    //    (a2,"a2.a3")
    //    (a3,"a3")
    //    (b1,"b1.b2")
    //    (b2,"b2")
    //    (c1,"c1")
    //
    //    childParentKVs (child,parent)
    //    (a1,"a2")
    //    (a2,"a3")
    //    (b1,"b2")
    //
    //    [iteration 0] nodePathKVs (next node,path)
    //    (a1,"a1")
    //    (a2,"a2")
    //    (a3,"a3")
    //    (b1,"b1")
    //    (b2,"b2")
    //    (c1,"c1")
    //    [iteration 0] nodeParentPathJoin (next node,paths,next node parents)
    //    (a1,["a1"],["a2"])
    //    (a2,["a2"],["a3"])
    //    (a3,["a3"],[])
    //    (b1,["b1"],["b2"])
    //    (b2,["b2"],[])
    //    (c1,["c1"],[])
    //
    //    [iteration 1] nodePathKVs (next node,path)
    //    (a2,"a2.a1")
    //    (a3,"a3.a2")
    //    (a3,"a3")
    //    (b2,"b2.b1")
    //    (b2,"b2")
    //    (c1,"c1")
    //    [iteration 1] nodeParentPathJoin (next node,paths,next node parents)
    //    (a1,[],["a2"])
    //    (a2,["a2.a1"],["a3"])
    //    (a3,["a3.a2","a3"],[])
    //    (b1,[],["b2]")
    //    (b2,["b2.b1","b2"],[])
    //    (c1,["c1"],[])

    // swap the key of all pairs from the next node in the path (which should all be root nodes at
    // this point), to the first node in the path
    PCollection<KV<Long, String>> nodePathKVs =
        nextNodePathKVs.apply(
            ParDo.of(
                new DoFn<KV<Long, String>, KV<Long, String>>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    KV<Long, String> kvPair = context.element();

                    String path = kvPair.getValue();

                    // strip out the first node in the path, and make it the key
                    // e.g. (a3,"a1.a2.a3") => (a1,"a2.a3")
                    //      (c1,"c1") => (c1,"")
                    int indexOfFirstPeriod = path.indexOf(".");
                    String firstNodeInPath;
                    String pathWithoutFirstNode;
                    if (indexOfFirstPeriod > 0) {
                      firstNodeInPath = path.substring(0, indexOfFirstPeriod);
                      pathWithoutFirstNode = path.substring(indexOfFirstPeriod + 1);
                    } else {
                      firstNodeInPath = path;
                      pathWithoutFirstNode = "";
                    }

                    Long firstNode;
                    try {
                      firstNode = Long.valueOf(firstNodeInPath);
                    } catch (NumberFormatException nfEx) {
                      firstNode = 0L;
                    }

                    context.output(KV.of(firstNode, pathWithoutFirstNode));
                  }
                }));

    // build the BQ row objects to insert
    PCollection<TableRow> nodePathBQRows =
        nodePathKVs.apply(
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

    // insert all these rows into BQ
    nodePathBQRows.apply(
        BigQueryIO.writeTableRows()
            .to(options.getOutputBigQueryTable())
            .withSchema(pathsForHierarchySchema(options))
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));

    pipeline.run().waitUntilFinish();
  }
}
