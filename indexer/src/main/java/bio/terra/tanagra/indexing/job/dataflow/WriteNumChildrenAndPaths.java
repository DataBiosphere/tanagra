package bio.terra.tanagra.indexing.job.dataflow;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.DataflowUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.PathUtils;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import bio.terra.tanagra.underlay.indextable.ITHierarchyChildParent;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STHierarchyRootFilter;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.bigquery.Table;
import jakarta.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.Create;
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
public class WriteNumChildrenAndPaths extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteNumChildrenAndPaths.class);
  private static final String TEMP_TABLE_NAME = "PNC";

  private final Entity entity;
  private final Hierarchy hierarchy;
  private final ITHierarchyChildParent indexChildParentTable;
  private final ITHierarchyAncestorDescendant indexAncestorDescendantTable;
  private final @Nullable STHierarchyRootFilter sourceRootFilterTable;
  private final ITEntityMain indexTable;

  public WriteNumChildrenAndPaths(
      SZIndexer indexerConfig,
      Entity entity,
      Hierarchy hierarchy,
      ITHierarchyChildParent indexChildParentTable,
      ITHierarchyAncestorDescendant indexAncestorDescendantTable,
      @Nullable STHierarchyRootFilter sourceRootFilterTable,
      ITEntityMain indexTable) {
    super(indexerConfig);
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.indexChildParentTable = indexChildParentTable;
    this.indexAncestorDescendantTable = indexAncestorDescendantTable;
    this.sourceRootFilterTable = sourceRootFilterTable;
    this.indexTable = indexTable;
  }

  @Override
  public String getEntity() {
    return entity.getName();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent()
            && outputTableHasAtLeastOneRowWithNotNullField(
                indexTable.getTablePointer(), indexTable.getHierarchyPathField(hierarchy.getName()))
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Only run the Dataflow job if the temp table hasn't been written yet.
    Optional<Table> tempTable =
        googleBigQuery.getTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getTempTableName());
    if (tempTable.isEmpty()) {
      writeFieldsToTempTable(isDryRun);
    } else {
      LOGGER.info("Temp table has already been written. Skipping Dataflow job.");
    }

    // Dataflow jobs can only write new rows to BigQuery, so in this second step, copy over the
    // path/numChildren values to the corresponding columns in the index entity main table.
    copyFieldsToEntityTable(isDryRun);
  }

  @Override
  public void clean(boolean isDryRun) {
    Optional<Table> tempTable =
        googleBigQuery.getTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getTempTableName());
    if (tempTable.isPresent()) {
      LOGGER.info("Deleting temp table: {}", tempTable.get().getFriendlyName());
      if (!isDryRun) {
        googleBigQuery.deleteTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getTempTableName());
      }
      LOGGER.info(
          "Temp table deleted. CreateEntityTable will delete the output table, which includes all the rows updated by this job.");
    } else {
      LOGGER.info(
          "Temp table not found. Nothing to delete. CreateEntityTable will delete the output table, which includes all the rows updated by this job.");
    }
  }

  public String getTempTableName() {
    // Define a temporary table to write the id/path/num_children information to.
    // We can't write directly to the entity table because the Beam BigQuery library doesn't support
    // updating existing rows.
    return new NameHelper(indexerConfig.bigQuery.indexData.tablePrefix)
        .getReservedTableName(TEMP_TABLE_NAME + "_" + entity.getName() + "_" + hierarchy.getName());
  }

  private void writeFieldsToTempTable(boolean isDryRun) {
    // Build the pipeline object from the Dataflow config.
    Pipeline pipeline = Pipeline.create(DataflowUtils.getPipelineOptions(indexerConfig, getName()));

    // Build a query to select all ids from the index entity main table, and the pipeline step to
    // read the results.
    String allIdsSql =
        "SELECT "
            + SqlQueryField.of(indexTable.getAttributeValueField(entity.getIdAttribute().getName()))
                .renderForSelect()
            + " FROM "
            + indexTable.getTablePointer().render();
    LOGGER.info("index all ids query: {}", allIdsSql);
    PCollection<Long> allNodesPC =
        BigQueryBeamUtils.readNodesFromBQ(
            pipeline, allIdsSql, entity.getIdAttribute().getName(), "allNodes");

    // Build a query to select all child-parent pairs from the source child-parent table, and the
    // pipeline step to read the results.
    StringBuilder childParentSql = new StringBuilder();
    childParentSql
        .append("SELECT * FROM ")
        .append(indexChildParentTable.getTablePointer().render());
    if (sourceRootFilterTable != null || !hierarchy.getRootNodeIds().isEmpty()) {
      // If there's a root node filter, then remove any parent-child relationships where the parent
      // isn't a descendant of a valid root node. This avoids us generating a path that points to an
      // invalid root node.
      childParentSql
          .append(" WHERE ")
          .append(SqlQueryField.of(indexChildParentTable.getParentField()).renderForSelect())
          .append(" IN ");

      if (sourceRootFilterTable != null) {
        // SELECT * FROM indexChildParent WHERE parent IN (SELECT root nodes UNION ALL SELECT
        // descendant FROM indexAncestorDescendant WHERE ancestor IN (SELECT root nodes))
        String selectRootNodeIds =
            "SELECT "
                + sourceRootFilterTable.getIdColumnSchema().getColumnName()
                + " FROM "
                + sourceRootFilterTable.getTablePointer().render();
        childParentSql
            .append('(')
            .append(selectRootNodeIds)
            .append(" UNION ALL SELECT ")
            .append(
                SqlQueryField.of(indexAncestorDescendantTable.getDescendantField())
                    .renderForSelect())
            .append(" FROM ")
            .append(indexAncestorDescendantTable.getTablePointer().render())
            .append(" WHERE ")
            .append(
                SqlQueryField.of(indexAncestorDescendantTable.getAncestorField()).renderForSelect())
            .append(" IN (")
            .append(selectRootNodeIds)
            .append("))");
      } else {
        // SELECT * FROM indexChildParent WHERE parent IN (root nodes) OR parent IN (SELECT
        // descendant FROM indexAncestorDescendant WHERE ancestor IN (root nodes))
        String rootNodeIds =
            hierarchy.getRootNodeIds().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        childParentSql
            .append('(')
            .append(rootNodeIds)
            .append(") OR ")
            .append(SqlQueryField.of(indexChildParentTable.getParentField()).renderForSelect())
            .append(" IN (SELECT ")
            .append(
                SqlQueryField.of(indexAncestorDescendantTable.getDescendantField())
                    .renderForSelect())
            .append(" FROM ")
            .append(indexAncestorDescendantTable.getTablePointer().render())
            .append(" WHERE ")
            .append(
                SqlQueryField.of(indexAncestorDescendantTable.getAncestorField()).renderForSelect())
            .append(" IN (")
            .append(rootNodeIds)
            .append("))");
      }
    }
    LOGGER.info("child-parent query: {}", childParentSql);
    PCollection<KV<Long, Long>> childParentRelationshipsPC =
        BigQueryBeamUtils.readTwoFieldRowsFromBQ(
            pipeline,
            childParentSql.toString(),
            indexChildParentTable.getChildField().getColumnName(),
            indexChildParentTable.getParentField().getColumnName());

    // Build the pipeline steps to compute a path to a root node for each node in the hierarchy.
    PCollection<KV<Long, String>> nodePathKVsPC =
        PathUtils.computePaths(allNodesPC, childParentRelationshipsPC, hierarchy.getMaxDepth());

    // Build the pipeline steps to count the number of children for each node in the hierarchy.
    PCollection<KV<Long, Long>> nodeNumChildrenKVsPC =
        PathUtils.countChildren(allNodesPC, childParentRelationshipsPC);

    PCollection<KV<Long, String>> nodePrunedPathKVsPC;
    if (hierarchy.isKeepOrphanNodes()) {
      nodePrunedPathKVsPC = nodePathKVsPC;
    } else {
      // Build the pipeline steps to prune orphan nodes from the hierarchy (i.e. set path=null for
      // nodes with no parents or children).
      nodePrunedPathKVsPC = PathUtils.pruneOrphanPaths(nodePathKVsPC, nodeNumChildrenKVsPC);
    }

    PCollection<KV<Long, String>> outputNodePathKVsPC;
    if (sourceRootFilterTable == null && hierarchy.getRootNodeIds().isEmpty()) {
      outputNodePathKVsPC = nodePrunedPathKVsPC;
    } else {
      PCollection<Long> possibleRootNodesPC;
      if (sourceRootFilterTable != null) {
        // Build a query to select all root node ids from the source root filter table, and the
        // pipeline step to read the results.
        String sourceRootFilterSql =
            "SELECT * FROM " + sourceRootFilterTable.getTablePointer().render();
        LOGGER.info("source root filter query: {}", sourceRootFilterSql);
        possibleRootNodesPC =
            BigQueryBeamUtils.readNodesFromBQ(
                pipeline,
                sourceRootFilterSql,
                sourceRootFilterTable.getIdColumnSchema().getColumnName(),
                "rootNodes");
      } else {
        // Build a PCollection from the list of individual root ids specified in the config.
        possibleRootNodesPC = pipeline.apply(Create.of(hierarchy.getRootNodeIds()));
      }

      // Build the pipeline steps to filter the root nodes (i.e. set path=null for any existing root
      // nodes that are not in the list of possibles)
      outputNodePathKVsPC = PathUtils.filterRootNodes(possibleRootNodesPC, nodePrunedPathKVsPC);
    }

    // Build the pipeline steps to write the node-{path, numChildren} pairs to the temp table.
    ColumnSchema idColumnSchema = indexTable.getAttributeValueColumnSchema(entity.getIdAttribute());
    ColumnSchema pathColumnSchema = indexTable.getHierarchyPathColumnSchema(hierarchy.getName());
    ColumnSchema numChildrenColumnSchema =
        indexTable.getHierarchyNumChildrenColumnSchema(hierarchy.getName());
    BQTable tempBQTable =
        new BQTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getTempTableName());
    writePathAndNumChildrenToBQ(
        idColumnSchema,
        pathColumnSchema,
        numChildrenColumnSchema,
        indexerConfig.bigQuery.indexData.projectId,
        indexerConfig.bigQuery.indexData.datasetId,
        tempBQTable,
        outputNodePathKVsPC,
        nodeNumChildrenKVsPC);

    // Kick off the pipeline.
    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  /** Write the {@link KV} pairs (id, path, num_children) to BQ. */
  @SuppressWarnings("checkstyle:ParameterNumber")
  private static void writePathAndNumChildrenToBQ(
      ColumnSchema idColumnSchema,
      ColumnSchema pathColumnSchema,
      ColumnSchema numChildrenColumnSchema,
      String indexProjectId,
      String indexDatasetId,
      BQTable tempBQTable,
      PCollection<KV<Long, String>> nodePathKVs,
      PCollection<KV<Long, Long>> nodeNumChildrenKVs) {
    // Define the CoGroupByKey tags.
    final TupleTag<String> pathTag = new TupleTag<>();
    final TupleTag<Long> numChildrenTag = new TupleTag<>();

    // Do a CoGroupByKey join of the current id-numChildren collection and the parent-child
    // collection.
    PCollection<KV<Long, CoGbkResult>> pathNumChildrenJoin =
        KeyedPCollectionTuple.of(pathTag, nodePathKVs)
            .and(numChildrenTag, nodeNumChildrenKVs)
            .apply(
                "join id-path and id-numChildren collections for BQ row generation",
                CoGroupByKey.create());

    // Build the schema for the temp table.
    List<TableFieldSchema> tempTableFieldSchemas =
        Stream.of(idColumnSchema, pathColumnSchema, numChildrenColumnSchema)
            .map(
                columnSchema ->
                    new TableFieldSchema()
                        .setName(columnSchema.getColumnName())
                        .setType(BigQueryBeamUtils.fromDataType(columnSchema.getDataType()).name())
                        .setMode(columnSchema.isRequired() ? "REQUIRED" : "NULLABLE"))
            .collect(Collectors.toList());
    TableSchema outputTableSchema = new TableSchema().setFields(tempTableFieldSchemas);

    // Run a ParDo for each row of the join result.
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
                            .set(idColumnSchema.getColumnName(), node)
                            .set(pathColumnSchema.getColumnName(), path)
                            .set(numChildrenColumnSchema.getColumnName(), numChildren));
                  }
                }));

    idPathAndNumChildrenBQRows.apply(
        "insert the (id, path, numChildren) rows into BQ",
        BigQueryIO.writeTableRows()
            .to(
                BigQueryBeamUtils.getTableSqlPath(
                    indexProjectId, indexDatasetId, tempBQTable.getTableName()))
            .withSchema(outputTableSchema)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  private void copyFieldsToEntityTable(boolean isDryRun) {
    // Build a query for the id-path-num_children tuples in the temp table.
    SqlField entityTableIdField =
        indexTable.getAttributeValueField(entity.getIdAttribute().getName());
    SqlField entityTablePathField = indexTable.getHierarchyPathField(hierarchy.getName());
    SqlField entityTableNumChildrenField =
        indexTable.getHierarchyNumChildrenField(hierarchy.getName());

    BQTable tempBQTable =
        new BQTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getTempTableName());
    SqlField tempTableIdField = SqlField.of(entityTableIdField.getColumnName());
    SqlField tempTablePathField = SqlField.of(entityTablePathField.getColumnName());
    SqlField tempTableNumChildrenField = SqlField.of(entityTableNumChildrenField.getColumnName());
    String tempTableSql =
        "SELECT "
            + SqlQueryField.of(tempTableIdField).renderForSelect()
            + ", "
            + SqlQueryField.of(tempTablePathField).renderForSelect()
            + ", "
            + SqlQueryField.of(tempTableNumChildrenField).renderForSelect()
            + " FROM "
            + tempBQTable.render();
    LOGGER.info("temp table query: {}", tempTableSql);

    // Build an update-from-select query for the index entity main table and the
    // id-path-num_children query.
    String updateTableAlias = "updatetable";
    String tempTableAlias = "temptable";
    String updateFromSelectSql =
        "UPDATE "
            + indexTable.getTablePointer().render()
            + " AS "
            + updateTableAlias
            + " SET "
            + SqlQueryField.of(entityTablePathField).renderForSelect(updateTableAlias)
            + " = "
            + SqlQueryField.of(tempTablePathField).renderForSelect(tempTableAlias)
            + ", "
            + SqlQueryField.of(entityTableNumChildrenField).renderForSelect(updateTableAlias)
            + " = "
            + SqlQueryField.of(tempTableNumChildrenField).renderForSelect(tempTableAlias)
            + " FROM (SELECT "
            + SqlQueryField.of(tempTablePathField).renderForSelect()
            + ", "
            + SqlQueryField.of(tempTableNumChildrenField).renderForSelect()
            + ", "
            + SqlQueryField.of(tempTableIdField).renderForSelect()
            + " FROM "
            + tempBQTable.render()
            + ") AS "
            + tempTableAlias
            + " WHERE "
            + SqlQueryField.of(entityTableIdField).renderForSelect(updateTableAlias)
            + " = "
            + SqlQueryField.of(tempTableIdField).renderForSelect(tempTableAlias);
    LOGGER.info("update-from-select query: {}", updateFromSelectSql);

    // Run the update-from-select to write the path and num_children fields in the index entity main
    // table.
    googleBigQuery.runInsertUpdateQuery(updateFromSelectSql, isDryRun);
  }
}
