package bio.terra.tanagra.indexing.job;

import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.CHILD_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.ID_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.NUMCHILDREN_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.PARENT_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.PATH_COLUMN_NAME;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.job.beam.BigQueryUtils;
import bio.terra.tanagra.indexing.job.beam.PathUtils;
import bio.terra.tanagra.underlay.Entity;
import bio.terra.tanagra.underlay.HierarchyMapping;
import bio.terra.tanagra.underlay.TablePointer;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.common.annotations.VisibleForTesting;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A batch Apache Beam pipeline for building a table that contains a path (i.e. a list of ancestors
 * in order) for each node in a hierarchy. Example row: (id,path)=(123,"456.789"), where 456 is the
 * parent of 123 and 789 is the grandparent of 123.
 */
public class BuildNumChildrenAndPaths extends BigQueryIndexingJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuildNumChildrenAndPaths.class);

  // The default table schema for the id-path-numChildren output table.
  private static final TableSchema PATH_NUMCHILDREN_TABLE_SCHEMA =
      new TableSchema()
          .setFields(
              List.of(
                  new TableFieldSchema()
                      .setName(ID_COLUMN_NAME)
                      .setType("INTEGER")
                      .setMode("REQUIRED"),
                  // TODO: Consider how to handle other node types besides integer. One possibility
                  // is to serialize a list into a string. Another possibility is to make the path
                  // column an INTEGER/REPEATED field instead of a string, although that may only
                  // work for BQ and not other backends.
                  new TableFieldSchema()
                      .setName(PATH_COLUMN_NAME)
                      .setType("STRING")
                      .setMode("NULLABLE"),
                  new TableFieldSchema()
                      .setName(NUMCHILDREN_COLUMN_NAME)
                      .setType("INTEGER")
                      .setMode("REQUIRED")));

  private final String hierarchyName;

  public BuildNumChildrenAndPaths(Entity entity, String hierarchyName) {
    super(entity);
    this.hierarchyName = hierarchyName;
  }

  @Override
  public String getName() {
    return "BUILD CHILDREN COUNTS AND PATHS (" + getEntity().getName() + ", " + hierarchyName + ")";
  }

  @Override
  protected void run(boolean isDryRun) {
    String selectAllIdsSql =
        getEntity()
            .getSourceDataMapping()
            .queryAttributes(Map.of(ID_COLUMN_NAME, getEntity().getIdAttribute()))
            .renderSQL();
    LOGGER.info("select all ids SQL: {}", selectAllIdsSql);

    HierarchyMapping sourceHierarchyMapping =
        getEntity().getSourceDataMapping().getHierarchyMapping(hierarchyName);
    String selectChildParentIdPairsSql =
        sourceHierarchyMapping
            .queryChildParentPairs(CHILD_COLUMN_NAME, PARENT_COLUMN_NAME)
            .renderSQL();
    LOGGER.info("select all child-parent id pairs SQL: {}", selectChildParentIdPairsSql);

    BigQueryDataset outputBQDataset = getOutputDataPointer();
    Pipeline pipeline = Pipeline.create(buildDataflowPipelineOptions(outputBQDataset));

    // read in the nodes and the child-parent relationships from BQ
    PCollection<Long> allNodesPC =
        BigQueryUtils.readNodesFromBQ(pipeline, selectAllIdsSql, "allNodes");
    PCollection<KV<Long, Long>> childParentRelationshipsPC =
        BigQueryUtils.readChildParentRelationshipsFromBQ(pipeline, selectChildParentIdPairsSql);

    // compute a path to a root node for each node in the hierarchy
    PCollection<KV<Long, String>> nodePathKVsPC =
        PathUtils.computePaths(allNodesPC, childParentRelationshipsPC, DEFAULT_MAX_HIERARCHY_DEPTH);

    // count the number of children for each node in the hierarchy
    PCollection<KV<Long, Long>> nodeNumChildrenKVsPC =
        PathUtils.countChildren(allNodesPC, childParentRelationshipsPC);

    // prune orphan nodes from the hierarchy (i.e. set path=null for nodes with no parents or
    // children)
    PCollection<KV<Long, String>> nodePrunedPathKVsPC =
        PathUtils.pruneOrphanPaths(nodePathKVsPC, nodeNumChildrenKVsPC);

    // filter the root nodes
    PCollection<KV<Long, String>> outputNodePathKVsPC =
        filterRootNodes(sourceHierarchyMapping, pipeline, nodePrunedPathKVsPC);

    // write the node-{path, numChildren} pairs to BQ
    writePathAndNumChildrenToBQ(outputNodePathKVsPC, nodeNumChildrenKVsPC, getOutputTablePointer());

    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  @Override
  @VisibleForTesting
  public TablePointer getOutputTablePointer() {
    return getEntity()
        .getIndexDataMapping()
        .getHierarchyMapping(hierarchyName)
        .getPathNumChildren()
        .getTablePointer();
  }

  /** Filter the root nodes, if a root nodes filter is specified by the hierarchy mapping. */
  private static PCollection<KV<Long, String>> filterRootNodes(
      HierarchyMapping sourceHierarchyMapping,
      Pipeline pipeline,
      PCollection<KV<Long, String>> nodePrunedPathKVsPC) {
    if (!sourceHierarchyMapping.hasRootNodesFilter()) {
      return nodePrunedPathKVsPC;
    }
    String selectPossibleRootIdsSql =
        sourceHierarchyMapping.queryPossibleRootNodes(ID_COLUMN_NAME).renderSQL();
    LOGGER.info("select possible root ids SQL: {}", selectPossibleRootIdsSql);

    // read in the possible root nodes from BQ
    PCollection<Long> possibleRootNodesPC =
        BigQueryUtils.readNodesFromBQ(pipeline, selectPossibleRootIdsSql, "rootNodes");

    // filter the root nodes (i.e. set path=null for any existing root nodes that are not in the
    // list of possibles)
    return PathUtils.filterRootNodes(possibleRootNodesPC, nodePrunedPathKVsPC);
  }

  /** Write the {@link KV} pairs (id, path, num_children) to BQ. */
  private static void writePathAndNumChildrenToBQ(
      PCollection<KV<Long, String>> nodePathKVs,
      PCollection<KV<Long, Long>> nodeNumChildrenKVs,
      TablePointer outputBQTable) {
    // define the CoGroupByKey tags
    final TupleTag<String> pathTag = new TupleTag<>();
    final TupleTag<Long> numChildrenTag = new TupleTag<>();

    // do a CoGroupByKey join of the current id-numChildren collection and the parent-child
    // collection
    PCollection<KV<Long, CoGbkResult>> pathNumChildrenJoin =
        KeyedPCollectionTuple.of(pathTag, nodePathKVs)
            .and(numChildrenTag, nodeNumChildrenKVs)
            .apply(
                "join id-path and id-numChildren collections for BQ row generation",
                CoGroupByKey.create());

    // run a ParDo for each row of the join result
    PCollection<TableRow> idPathAndNumChildrenBQRows =
        pathNumChildrenJoin.apply(
            "run ParDo for each row of the id-path and id-numChildren join result to build the BQ (id, path, numChildren) row objects",
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

                    context.output(
                        new TableRow()
                            .set(ID_COLUMN_NAME, node)
                            .set(PATH_COLUMN_NAME, path)
                            .set(NUMCHILDREN_COLUMN_NAME, numChildren));
                  }
                }));

    idPathAndNumChildrenBQRows.apply(
        "insert the (id, path, numChildren) rows into BQ",
        BigQueryIO.writeTableRows()
            .to(outputBQTable.getPathForIndexing())
            .withSchema(PATH_NUMCHILDREN_TABLE_SCHEMA)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }
}
