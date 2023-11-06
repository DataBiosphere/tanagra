package bio.terra.tanagra.indexing.job.dataflow;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.DataflowUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.PathUtils;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.UpdateFromSelect;
import bio.terra.tanagra.query.bigquery.BigQueryDataset;
import bio.terra.tanagra.underlay2.NameHelper;
import bio.terra.tanagra.underlay2.entitymodel.Entity;
import bio.terra.tanagra.underlay2.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import bio.terra.tanagra.underlay2.sourcetable.STHierarchyChildParent;
import bio.terra.tanagra.underlay2.sourcetable.STHierarchyRootFilter;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.bigquery.Table;
import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
  private final STHierarchyChildParent sourceChildParentTable;
  private final @Nullable STHierarchyRootFilter sourceRootFilterTable;
  private final ITEntityMain indexTable;

  public WriteNumChildrenAndPaths(
      SZIndexer indexerConfig,
      Entity entity,
      Hierarchy hierarchy,
      STHierarchyChildParent sourceChildParentTable,
      @Nullable STHierarchyRootFilter sourceRootFilterTable,
      ITEntityMain indexTable) {
    super(indexerConfig);
    this.entity = entity;
    this.hierarchy = hierarchy;
    this.sourceChildParentTable = sourceChildParentTable;
    this.sourceRootFilterTable = sourceRootFilterTable;
    this.indexTable = indexTable;
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent()
            && outputTableHasAtLeastOneRowWithNotNullField(
                indexTable.getHierarchyPathField(hierarchy.getName()),
                indexTable.getHierarchyPathColumnSchema(hierarchy.getName()))
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Only run the Dataflow job if the temp table hasn't been written yet.
    Optional<Table> tempTable =
        bigQueryExecutor
            .getBigQueryService()
            .getTable(
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
        bigQueryExecutor
            .getBigQueryService()
            .getTable(
                indexerConfig.bigQuery.indexData.projectId,
                indexerConfig.bigQuery.indexData.datasetId,
                getTempTableName());
    if (tempTable.isPresent()) {
      LOGGER.info("Deleting temp table: {}", tempTable.get().getFriendlyName());
      if (!isDryRun) {
        bigQueryExecutor
            .getBigQueryService()
            .deleteTable(
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
    TableVariable indexEntityMainTable = TableVariable.forPrimary(indexTable.getTablePointer());
    List<TableVariable> indexEntityMainTableVars = Lists.newArrayList(indexEntityMainTable);
    FieldVariable indexEntityMainIdFieldVar =
        indexTable
            .getAttributeValueField(entity.getIdAttribute().getName())
            .buildVariable(indexEntityMainTable, indexEntityMainTableVars);
    Query allIdsQuery =
        new Query.Builder()
            .select(List.of(indexEntityMainIdFieldVar))
            .tables(indexEntityMainTableVars)
            .build();
    LOGGER.info("index all ids query: {}", allIdsQuery.renderSQL());
    PCollection<Long> allNodesPC =
        BigQueryBeamUtils.readNodesFromBQ(
            pipeline, allIdsQuery.renderSQL(), entity.getIdAttribute().getName(), "allNodes");

    // Build a query to select all child-parent pairs from the source child-parent table, and the
    // pipeline step to read the results.
    Query sourceChildParentQuery = sourceChildParentTable.getQueryAll(Map.of());
    LOGGER.info("source child-parent query: {}", sourceChildParentQuery.renderSQL());
    PCollection<KV<Long, Long>> childParentRelationshipsPC =
        BigQueryBeamUtils.readTwoFieldRowsFromBQ(
            pipeline,
            sourceChildParentQuery.renderSQL(),
            sourceChildParentTable.getChildColumnSchema().getColumnName(),
            sourceChildParentTable.getParentColumnSchema().getColumnName());

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
        Query sourceRootFilterQuery = sourceRootFilterTable.getQueryAll(Map.of());
        LOGGER.info("source root filter query: {}", sourceRootFilterQuery.renderSQL());
        possibleRootNodesPC =
            BigQueryBeamUtils.readNodesFromBQ(
                pipeline,
                sourceRootFilterQuery.renderSQL(),
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
    TablePointer tempTablePointer =
        TablePointer.fromTableName(
            getTempTableName(), indexTable.getTablePointer().getDataPointer());
    writePathAndNumChildrenToBQ(
        idColumnSchema,
        pathColumnSchema,
        numChildrenColumnSchema,
        tempTablePointer,
        outputNodePathKVsPC,
        nodeNumChildrenKVsPC);

    // Kick off the pipeline.
    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  /** Write the {@link KV} pairs (id, path, num_children) to BQ. */
  private static void writePathAndNumChildrenToBQ(
      ColumnSchema idColumnSchema,
      ColumnSchema pathColumnSchema,
      ColumnSchema numChildrenColumnSchema,
      TablePointer tempTablePointer,
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
        List.of(idColumnSchema, pathColumnSchema, numChildrenColumnSchema).stream()
            .map(
                columnSchema ->
                    new TableFieldSchema()
                        .setName(columnSchema.getColumnName())
                        .setType(
                            BigQueryDataset.fromSqlDataType(columnSchema.getSqlDataType()).name())
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
            .to(tempTablePointer.getPathForIndexing())
            .withSchema(outputTableSchema)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  private void copyFieldsToEntityTable(boolean isDryRun) {
    // Build a query for the id-path-num_children tuples in the temp table.
    TablePointer tempTablePointer =
        TablePointer.fromTableName(
            getTempTableName(), indexTable.getTablePointer().getDataPointer());
    TableVariable tempTableVar = TableVariable.forPrimary(tempTablePointer);
    List<TableVariable> tempTableVars = Lists.newArrayList(tempTableVar);
    ColumnSchema idColumnSchema = indexTable.getAttributeValueColumnSchema(entity.getIdAttribute());
    ColumnSchema pathColumnSchema = indexTable.getHierarchyPathColumnSchema(hierarchy.getName());
    ColumnSchema numChildrenColumnSchema =
        indexTable.getHierarchyNumChildrenColumnSchema(hierarchy.getName());
    FieldVariable tempTableIdFieldVar =
        new FieldPointer.Builder()
            .tablePointer(tempTablePointer)
            .columnName(idColumnSchema.getColumnName())
            .build()
            .buildVariable(tempTableVar, tempTableVars);
    FieldVariable tempTablePathFieldVar =
        new FieldPointer.Builder()
            .tablePointer(tempTablePointer)
            .columnName(pathColumnSchema.getColumnName())
            .build()
            .buildVariable(tempTableVar, tempTableVars);
    FieldVariable tempTableNumChildrenFieldVar =
        new FieldPointer.Builder()
            .tablePointer(tempTablePointer)
            .columnName(numChildrenColumnSchema.getColumnName())
            .build()
            .buildVariable(tempTableVar, tempTableVars);
    Query tempTableQuery =
        new Query.Builder()
            .select(
                List.of(tempTableIdFieldVar, tempTablePathFieldVar, tempTableNumChildrenFieldVar))
            .tables(tempTableVars)
            .build();
    LOGGER.info("temp table query: {}", tempTableQuery.renderSQL());

    // Build an update-from-select query for the index entity main table and the
    // id-path-num_children query.
    TableVariable updateTable = TableVariable.forPrimary(indexTable.getTablePointer());
    List<TableVariable> updateTableVars = Lists.newArrayList(updateTable);
    FieldVariable updateTablePathFieldVar =
        indexTable
            .getHierarchyPathField(hierarchy.getName())
            .buildVariable(updateTable, updateTableVars);
    FieldVariable updateTableNumChildrenFieldVar =
        indexTable
            .getHierarchyNumChildrenField(hierarchy.getName())
            .buildVariable(updateTable, updateTableVars);
    Map<FieldVariable, FieldVariable> updateFields =
        Map.of(
            updateTablePathFieldVar, tempTablePathFieldVar,
            updateTableNumChildrenFieldVar, tempTableNumChildrenFieldVar);
    FieldVariable updateTableIdFieldVar =
        indexTable
            .getAttributeValueField(entity.getIdAttribute().getName())
            .buildVariable(updateTable, updateTableVars);
    UpdateFromSelect updateQuery =
        new UpdateFromSelect(
            updateTable, updateFields, tempTableQuery, updateTableIdFieldVar, tempTableIdFieldVar);
    LOGGER.info("update-from-select query: {}", updateQuery.renderSQL());

    // Run the update-from-select to write the path and num_children fields in the index entity main
    // table.
    bigQueryExecutor.getBigQueryService().runInsertUpdateQuery(updateQuery.renderSQL(), isDryRun);
  }
}
