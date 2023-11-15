package bio.terra.tanagra.indexing.job.dataflow;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.CountUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.DataflowUtils;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.UpdateFromSelect;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.bigquery.Table;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Count the number of distinct occurrences for entity, which may optionally include a hierarchy,
 * and writes the results to the index entity BQ table.
 *
 * <p>This job is called 4 times for condition_person_occurrence entity group. For SDD condition
 * 22274:
 *
 * <pre>
 * criteriaToPrimary relationship, no hierarchy:
 *
 *     t_count_person column = 755 will be added to index condition table, because 775 people have
 *          a condition occurrence entity with condition 22274
 *
 * criteriaToPrimary relationship, standard hierarchy:
 *
 *     t_count_person_standard: 775 people have an occurrence entity with condition 22274 or a
 *         condition below it in the hierarchy
 *
 * criteriaToOccurrence, no hierarchy:
 *
 *   t_count_condition_occurrence: 3379 occurrences (across all people) for condition 22274
 *
 * criteriaToOccurrence, standard hierarchy:
 *
 *   t_count_condition_occurrence_standard: 3380 occurrences (across all people) for condition
 * 22274
 *       or conditions below 22274 in the hierarchy
 * </pre>
 */
public class WriteRollupCounts extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteRollupCounts.class);

  private static final String TEMP_TABLE_NAME = "RC";

  private final EntityGroup entityGroup;
  private final Entity entity;
  private final Entity countedEntity;
  private final Relationship relationship;
  private final ITEntityMain indexTable;
  private final ITEntityMain countedEntityIndexTable;
  private final @Nullable ITRelationshipIdPairs relationshipIdPairsIndexTable;
  private final @Nullable Hierarchy hierarchy;
  private final @Nullable ITHierarchyAncestorDescendant ancestorDescendantTable;

  @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
  public WriteRollupCounts(
      SZIndexer indexerConfig,
      EntityGroup entityGroup,
      Entity entity,
      Entity countedEntity,
      Relationship relationship,
      ITEntityMain indexTable,
      ITEntityMain countedEntityIndexTable,
      @Nullable ITRelationshipIdPairs relationshipIdPairsIndexTable,
      @Nullable Hierarchy hierarchy,
      @Nullable ITHierarchyAncestorDescendant ancestorDescendantTable) {
    super(indexerConfig);
    this.entityGroup = entityGroup;
    this.entity = entity;
    this.countedEntity = countedEntity;
    this.relationship = relationship;
    this.indexTable = indexTable;
    this.countedEntityIndexTable = countedEntityIndexTable;
    this.relationshipIdPairsIndexTable = relationshipIdPairsIndexTable;
    this.hierarchy = hierarchy;
    this.ancestorDescendantTable = ancestorDescendantTable;
  }

  @Override
  public String getName() {
    return String.format(
        "%s-%s-%s",
        this.getClass().getSimpleName(),
        getOutputTableName(),
        hierarchy == null ? ITEntityMain.NO_HIERARCHY_NAME : hierarchy.getName());
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent()
            && outputTableHasAtLeastOneRowWithNotNullField(
                indexTable.getEntityGroupCountField(
                    entityGroup.getName(), hierarchy == null ? null : hierarchy.getName()),
                indexTable.getEntityGroupCountColumnSchema(
                    entityGroup.getName(), hierarchy == null ? null : hierarchy.getName()))
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
    // count values to the corresponding column in the index entity main table.
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
    // Define a temporary table to write the id/count information to.
    // We can't write directly to the entity table because the Beam BigQuery library doesn't support
    // updating existing rows.
    return new NameHelper(indexerConfig.bigQuery.indexData.tablePrefix)
        .getReservedTableName(
            TEMP_TABLE_NAME
                + "_"
                + entityGroup.getName()
                + "_"
                + (hierarchy == null ? ITEntityMain.NO_HIERARCHY_NAME : hierarchy.getName()));
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

    // Build a query to select all entity-countedEntity id pairs from the index table, and the
    // pipeline step to read the results.
    final String entityIdColumnName = "entityId";
    final String countedEntityIdColumnName = "countedEntityId";
    Query idPairsQuery;
    if (relationship.isForeignKeyAttribute(entity)) {
      FieldVariable entityIdFieldVar =
          indexTable
              .getAttributeValueField(entity.getIdAttribute().getName())
              .buildVariable(indexEntityMainTable, indexEntityMainTableVars, entityIdColumnName);
      FieldVariable countedEntityIdFieldVar =
          indexTable
              .getAttributeValueField(relationship.getForeignKeyAttribute(entity).getName())
              .buildVariable(
                  indexEntityMainTable, indexEntityMainTableVars, countedEntityIdColumnName);
      idPairsQuery =
          new Query.Builder()
              .select(List.of(entityIdFieldVar, countedEntityIdFieldVar))
              .tables(indexEntityMainTableVars)
              .build();
    } else if (relationship.isForeignKeyAttribute(countedEntity)) {
      TableVariable countedEntityMainTable =
          TableVariable.forPrimary(countedEntityIndexTable.getTablePointer());
      List<TableVariable> countedEntityMainTableVars = Lists.newArrayList(countedEntityMainTable);
      FieldVariable entityIdFieldVar =
          countedEntityIndexTable
              .getAttributeValueField(relationship.getForeignKeyAttribute(countedEntity).getName())
              .buildVariable(indexEntityMainTable, indexEntityMainTableVars, entityIdColumnName);
      FieldVariable countedEntityIdFieldVar =
          countedEntityIndexTable
              .getAttributeValueField(countedEntity.getIdAttribute().getName())
              .buildVariable(
                  countedEntityMainTable, countedEntityMainTableVars, countedEntityIdColumnName);
      idPairsQuery =
          new Query.Builder()
              .select(List.of(entityIdFieldVar, countedEntityIdFieldVar))
              .tables(countedEntityMainTableVars)
              .build();
    } else { // relationship.isIntermediateTable()
      idPairsQuery =
          relationshipIdPairsIndexTable.getQueryAll(
              Map.of(
                  ITRelationshipIdPairs.Column.ENTITY_A_ID.getSchema(),
                      relationship.getEntityA().equals(entity)
                          ? entityIdColumnName
                          : countedEntityIdColumnName,
                  ITRelationshipIdPairs.Column.ENTITY_B_ID.getSchema(),
                      relationship.getEntityB().equals(entity)
                          ? entityIdColumnName
                          : countedEntityIdColumnName));
    }
    LOGGER.info("index entity-countedEntity id pairs query: {}", idPairsQuery.renderSQL());
    PCollection<KV<Long, Long>> idPairsPC =
        BigQueryBeamUtils.readTwoFieldRowsFromBQ(
            pipeline, idPairsQuery.renderSQL(), entityIdColumnName, countedEntityIdColumnName);

    // Optionally handle a hierarchy for the rollup entity.
    if (hierarchy != null) {
      // Build a query to select all ancestor-descendant pairs from the ancestor-descendant table,
      // and the pipeline step to read the results.
      Query ancestorDescendantQuery = ancestorDescendantTable.getQueryAll(Map.of());
      LOGGER.info("ancestor-descendant query: {}", ancestorDescendantQuery.renderSQL());
      PCollection<KV<Long, Long>> ancestorDescendantRelationshipsPC =
          BigQueryBeamUtils.readTwoFieldRowsFromBQ(
              pipeline,
              ancestorDescendantQuery.renderSQL(),
              ITHierarchyAncestorDescendant.Column.DESCENDANT.getSchema().getColumnName(),
              ITHierarchyAncestorDescendant.Column.ANCESTOR.getSchema().getColumnName());

      // Expand the set of occurrences to include a repeat for each ancestor.
      idPairsPC =
          CountUtils.repeatOccurrencesForHierarchy(idPairsPC, ancestorDescendantRelationshipsPC);
    }

    // Count the number of distinct occurrences per entity id.
    PCollection<KV<Long, Long>> nodeCountKVsPC = CountUtils.countDistinct(allNodesPC, idPairsPC);

    // Build the pipeline steps to write the node-count pairs to the temp table.
    ColumnSchema idColumnSchema = indexTable.getAttributeValueColumnSchema(entity.getIdAttribute());
    ColumnSchema countColumnSchema =
        indexTable.getEntityGroupCountColumnSchema(
            entityGroup.getName(), hierarchy == null ? null : hierarchy.getName());
    TablePointer tempTablePointer =
        new TablePointer(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getTempTableName());
    writeCountsToBQ(
        idColumnSchema,
        countColumnSchema,
        indexerConfig.bigQuery.indexData.projectId,
        indexerConfig.bigQuery.indexData.datasetId,
        tempTablePointer,
        nodeCountKVsPC);

    // Kick off the pipeline.
    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  /** Write the {@link KV} pairs (id, rollup_count) to BQ. */
  private static void writeCountsToBQ(
      ColumnSchema idColumnSchema,
      ColumnSchema countColumnSchema,
      String indexProjectId,
      String indexDatasetId,
      TablePointer tempTablePointer,
      PCollection<KV<Long, Long>> nodeCountKVs) {
    // Build the schema for the temp table.
    List<TableFieldSchema> tempTableFieldSchemas =
        List.of(idColumnSchema, countColumnSchema).stream()
            .map(
                columnSchema ->
                    new TableFieldSchema()
                        .setName(columnSchema.getColumnName())
                        .setType(
                            BigQueryBeamUtils.fromSqlDataType(columnSchema.getSqlDataType()).name())
                        .setMode(columnSchema.isRequired() ? "REQUIRED" : "NULLABLE"))
            .collect(Collectors.toList());
    TableSchema outputTableSchema = new TableSchema().setFields(tempTableFieldSchemas);

    PCollection<TableRow> nodeCountBQRows =
        nodeCountKVs.apply(
            "build (id, count) pcollection of BQ rows",
            ParDo.of(
                new DoFn<KV<Long, Long>, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    KV<Long, Long> element = context.element();
                    context.output(
                        new TableRow()
                            .set(idColumnSchema.getColumnName(), element.getKey())
                            .set(countColumnSchema.getColumnName(), element.getValue()));
                  }
                }));

    nodeCountBQRows.apply(
        "insert the (id, rollup_count, rollup_displayHints) rows into BQ",
        BigQueryIO.writeTableRows()
            .to(
                BigQueryBeamUtils.getTableSqlPath(
                    indexProjectId, indexDatasetId, tempTablePointer.getTableName()))
            .withSchema(outputTableSchema)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  private void copyFieldsToEntityTable(boolean isDryRun) {
    // Build a query for the id-count pairs in the temp table.
    TablePointer tempTablePointer =
        new TablePointer(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getTempTableName());
    TableVariable tempTableVar = TableVariable.forPrimary(tempTablePointer);
    List<TableVariable> tempTableVars = Lists.newArrayList(tempTableVar);
    ColumnSchema idColumnSchema = indexTable.getAttributeValueColumnSchema(entity.getIdAttribute());
    ColumnSchema columnColumnSchema =
        indexTable.getEntityGroupCountColumnSchema(
            entityGroup.getName(), hierarchy == null ? null : hierarchy.getName());
    FieldVariable tempTableIdFieldVar =
        new FieldPointer.Builder()
            .tablePointer(tempTablePointer)
            .columnName(idColumnSchema.getColumnName())
            .build()
            .buildVariable(tempTableVar, tempTableVars);
    FieldVariable tempTableCountFieldVar =
        new FieldPointer.Builder()
            .tablePointer(tempTablePointer)
            .columnName(columnColumnSchema.getColumnName())
            .build()
            .buildVariable(tempTableVar, tempTableVars);
    Query tempTableQuery =
        new Query.Builder()
            .select(List.of(tempTableIdFieldVar, tempTableCountFieldVar))
            .tables(tempTableVars)
            .build();
    LOGGER.info("temp table query: {}", tempTableQuery.renderSQL());

    // Build an update-from-select query for the index entity main table and the
    // id-count query.
    TableVariable updateTable = TableVariable.forPrimary(indexTable.getTablePointer());
    List<TableVariable> updateTableVars = Lists.newArrayList(updateTable);
    FieldVariable updateTableCountFieldVar =
        indexTable
            .getEntityGroupCountField(
                entityGroup.getName(), hierarchy == null ? null : hierarchy.getName())
            .buildVariable(updateTable, updateTableVars);
    Map<FieldVariable, FieldVariable> updateFields =
        Map.of(updateTableCountFieldVar, tempTableCountFieldVar);
    FieldVariable updateTableIdFieldVar =
        indexTable
            .getAttributeValueField(entity.getIdAttribute().getName())
            .buildVariable(updateTable, updateTableVars);
    UpdateFromSelect updateQuery =
        new UpdateFromSelect(
            updateTable, updateFields, tempTableQuery, updateTableIdFieldVar, tempTableIdFieldVar);
    LOGGER.info("update-from-select query: {}", updateQuery.renderSQL());

    // Run the update-from-select to write the count field in the index entity main table.
    bigQueryExecutor.getBigQueryService().runInsertUpdateQuery(updateQuery.renderSQL(), isDryRun);
    //    // Build a query for the id-rollup_count-rollup_displayHints tuples that we want to
    // select.
    //    Query idCountDisplayHintsTuples = queryIdRollupTuples(getTempTable());
    //    LOGGER.info(
    //        "select all id-count-displayHints tuples SQL: {}",
    // idCountDisplayHintsTuples.renderSQL());
    //
    //    // Build a map of (output) update field name -> (input) selected FieldVariable.
    //    // This map only contains two items, because we're only updating the count and
    // display_hints
    //    // fields.
    //    RelationshipMapping indexMapping = relationship.getMapping(Underlay.MappingType.INDEX);
    //    Map<String, FieldVariable> updateFields = new HashMap<>();
    //
    //    String updateCountFieldName =
    //        indexMapping.getRollupInfo(getRollupEntity(), hierarchy).getCount().getColumnName();
    //    FieldVariable selectCountField =
    //        idCountDisplayHintsTuples.getSelect().stream()
    //            .filter(fv -> fv.getAliasOrColumnName().equals(ROLLUP_COUNT_COLUMN_NAME))
    //            .findFirst()
    //            .get();
    //    updateFields.put(updateCountFieldName, selectCountField);
    //
    //    String updateDisplayHintsFieldName =
    //        indexMapping.getRollupInfo(getRollupEntity(),
    // hierarchy).getDisplayHints().getColumnName();
    //    FieldVariable selectDisplayHintsField =
    //        idCountDisplayHintsTuples.getSelect().stream()
    //            .filter(fv -> fv.getAliasOrColumnName().equals(ROLLUP_DISPLAY_HINTS_COLUMN_NAME))
    //            .findFirst()
    //            .get();
    //    updateFields.put(updateDisplayHintsFieldName, selectDisplayHintsField);
    //
    //    // Check that the count and display_hints fields are not in a different table from the
    // entity
    //    // table.
    //    if (indexMapping.getRollupInfo(getRollupEntity(), hierarchy).getCount().isForeignKey()
    //        || !indexMapping
    //            .getRollupInfo(getRollupEntity(), hierarchy)
    //            .getCount()
    //            .getTablePointer()
    //            .equals(getEntity().getMapping(Underlay.MappingType.INDEX).getTablePointer())) {
    //      throw new SystemException(
    //          "Indexing rollup count information only supports an index mapping to a column in the
    // entity table");
    //    }
    //    if (indexMapping.getRollupInfo(getRollupEntity(),
    // hierarchy).getDisplayHints().isForeignKey()
    //        || !indexMapping
    //            .getRollupInfo(getRollupEntity(), hierarchy)
    //            .getDisplayHints()
    //            .getTablePointer()
    //            .equals(getEntity().getMapping(Underlay.MappingType.INDEX).getTablePointer())) {
    //      throw new SystemException(
    //          "Indexing rollup display hints information only supports an index mapping to a
    // column in
    // the entity table");
    //    }
    //
    //    updateEntityTableFromSelect(idCountDisplayHintsTuples, updateFields, ID_COLUMN_NAME,
    // isDryRun);
  }
}
