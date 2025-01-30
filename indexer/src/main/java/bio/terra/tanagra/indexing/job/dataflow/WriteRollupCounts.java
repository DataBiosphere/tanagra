package bio.terra.tanagra.indexing.job.dataflow;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.CountUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.DataflowUtils;
import bio.terra.tanagra.query.bigquery.BQTable;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlQueryField;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.entitymodel.Hierarchy;
import bio.terra.tanagra.underlay.entitymodel.Relationship;
import bio.terra.tanagra.underlay.entitymodel.entitygroup.EntityGroup;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
import bio.terra.tanagra.underlay.indextable.ITRelationshipIdPairs;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STRelationshipRollupCounts;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Table;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.lang3.StringUtils;
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
  private static final String INDEX_ID_REGEX_BRACES = "{indexIdRegex}";
  private final EntityGroup entityGroup;
  private final Entity entity;
  private final Entity countedEntity;
  private final Relationship relationship;
  private final ITEntityMain indexTable;
  private final ITEntityMain countedEntityIndexTable;
  private final @Nullable ITRelationshipIdPairs relationshipIdPairsIndexTable;
  private final @Nullable Hierarchy hierarchy;
  private final @Nullable ITHierarchyAncestorDescendant ancestorDescendantTable;
  private final @Nullable STRelationshipRollupCounts relationshipRollupCountsSourceTable;

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
      @Nullable ITHierarchyAncestorDescendant ancestorDescendantTable,
      @Nullable STRelationshipRollupCounts relationshipRollupCountsSourceTable) {
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
    this.relationshipRollupCountsSourceTable = relationshipRollupCountsSourceTable;
  }

  @Override
  public String getEntityGroup() {
    return entityGroup.getName();
  }

  @Override
  public String getName() {
    return String.format(
        "%s-%s-%s-%s",
        this.getClass().getSimpleName(),
        entityGroup.getName(),
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
                indexTable.getTablePointer(),
                indexTable.getEntityGroupCountField(
                    entityGroup.getName(), hierarchy == null ? null : hierarchy.getName()))
        ? JobStatus.COMPLETE
        : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    if (relationshipRollupCountsSourceTable == null) {
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
    } else {
      LOGGER.info("Rollup counts source SQL is defined. Skipping Dataflow job.");
    }

    final int maxRetries = 5;
    var attempt = 0;

    while (attempt < maxRetries) {
      try {
        // Dataflow jobs can only write new rows to BigQuery, so in this second step, copy over the
        // count values to the corresponding column in the index entity main table.
        copyFieldsToEntityTable(isDryRun);
        return; // Exit if successful
      } catch (BigQueryException e) {
        if (e.getMessage().contains("due to concurrent update")) {
          attempt++;
          LOGGER.info("Attempt {} failed: {}", attempt, e.getMessage());

          if (attempt == maxRetries) {
            LOGGER.info("Max retries reached. Giving up.");
            throw e; // Rethrow after max attempts
          }

          try {
            Thread.sleep(1000); // Sleep for 1 second
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", ie);
          }
        } else {
          throw e; // Rethrow if it's a different exception
        }
      }
    }
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

    // Build a query to select all entity-countedEntity id pairs from the index table, and the
    // pipeline step to read the results.
    final String entityIdColumnName = "entityId";
    final String countedEntityIdColumnName = "countedEntityId";
    String idPairsSql;
    if (relationship.isForeignKeyAttribute(entity)) {
      SqlField entityIdField = indexTable.getAttributeValueField(entity.getIdAttribute().getName());
      SqlField countedEntityIdField =
          indexTable.getAttributeValueField(relationship.getForeignKeyAttribute(entity).getName());
      idPairsSql =
          "SELECT "
              + SqlQueryField.of(entityIdField, entityIdColumnName).renderForSelect()
              + ", "
              + SqlQueryField.of(countedEntityIdField, countedEntityIdColumnName).renderForSelect()
              + " FROM "
              + indexTable.getTablePointer().render();
    } else if (relationship.isForeignKeyAttribute(countedEntity)) {
      SqlField entityIdField =
          countedEntityIndexTable.getAttributeValueField(
              relationship.getForeignKeyAttribute(countedEntity).getName());
      SqlField countedEntityIdField =
          countedEntityIndexTable.getAttributeValueField(countedEntity.getIdAttribute().getName());
      idPairsSql =
          "SELECT "
              + SqlQueryField.of(entityIdField, entityIdColumnName).renderForSelect()
              + ", "
              + SqlQueryField.of(countedEntityIdField, countedEntityIdColumnName).renderForSelect()
              + " FROM "
              + countedEntityIndexTable.getTablePointer().render();
    } else { // relationship.isIntermediateTable()
      SqlField entityIdField = relationshipIdPairsIndexTable.getEntityIdField(entity.getName());
      SqlField countedEntityIdField =
          relationshipIdPairsIndexTable.getEntityIdField(countedEntity.getName());
      idPairsSql =
          "SELECT "
              + SqlQueryField.of(entityIdField, entityIdColumnName).renderForSelect()
              + ", "
              + SqlQueryField.of(countedEntityIdField, countedEntityIdColumnName).renderForSelect()
              + " FROM "
              + relationshipIdPairsIndexTable.getTablePointer().render();
    }
    LOGGER.info("index entity-countedEntity id pairs query: {}", idPairsSql);
    PCollection<KV<Long, Long>> idPairsPC =
        BigQueryBeamUtils.readTwoFieldRowsFromBQ(
            pipeline, idPairsSql, entityIdColumnName, countedEntityIdColumnName);

    // Optionally handle a hierarchy for the rollup entity.
    if (hierarchy != null) {
      PCollection<KV<Long, Long>> descendantAncestorRelationshipsPC =
          BigQueryBeamUtils.readDescendantAncestorRelationshipsFromBQ(
              pipeline, ancestorDescendantTable);

      // Expand the set of occurrences to include a repeat for each ancestor.
      idPairsPC =
          CountUtils.repeatOccurrencesForHierarchy(idPairsPC, descendantAncestorRelationshipsPC);
    }

    // Count the number of distinct occurrences per entity id.
    PCollection<KV<Long, Long>> nodeCountKVsPC = CountUtils.countDistinct(allNodesPC, idPairsPC);

    // Build the pipeline steps to write the node-count pairs to the temp table.
    ColumnSchema idColumnSchema = indexTable.getAttributeValueColumnSchema(entity.getIdAttribute());
    ColumnSchema countColumnSchema =
        indexTable.getEntityGroupCountColumnSchema(
            entityGroup.getName(), hierarchy == null ? null : hierarchy.getName());
    BQTable tempBQTable =
        new BQTable(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getTempTableName());
    writeCountsToBQ(
        idColumnSchema,
        countColumnSchema,
        indexerConfig.bigQuery.indexData.projectId,
        indexerConfig.bigQuery.indexData.datasetId,
        tempBQTable,
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
      BQTable tempBQTable,
      PCollection<KV<Long, Long>> nodeCountKVs) {
    // Build the schema for the temp table.
    List<TableFieldSchema> tempTableFieldSchemas =
        Stream.of(idColumnSchema, countColumnSchema)
            .map(
                columnSchema ->
                    new TableFieldSchema()
                        .setName(columnSchema.getColumnName())
                        .setType(BigQueryBeamUtils.fromDataType(columnSchema.getDataType()).name())
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
                    indexProjectId, indexDatasetId, tempBQTable.getTableName()))
            .withSchema(outputTableSchema)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  private void copyFieldsToEntityTable(boolean isDryRun) {
    // Build a query for the id-count pairs in the temp table.
    SqlField entityTableIdField =
        indexTable.getAttributeValueField(entity.getIdAttribute().getName());
    SqlField entityTableCountField =
        indexTable.getEntityGroupCountField(
            entityGroup.getName(), hierarchy == null ? null : hierarchy.getName());

    BQTable rollupCountsBQTable;
    SqlField rollupCountsTableIdField;
    SqlField rollupCountsTableCountField;
    if (relationshipRollupCountsSourceTable == null) {
      rollupCountsBQTable =
          new BQTable(
              indexerConfig.bigQuery.indexData.projectId,
              indexerConfig.bigQuery.indexData.datasetId,
              getTempTableName());
      rollupCountsTableIdField = SqlField.of(entityTableIdField.getColumnName());
      rollupCountsTableCountField = SqlField.of(entityTableCountField.getColumnName());
    } else {
      rollupCountsBQTable = relationshipRollupCountsSourceTable.getTablePointer();
      rollupCountsTableIdField = relationshipRollupCountsSourceTable.getEntityIdField();
      rollupCountsTableCountField = relationshipRollupCountsSourceTable.getCountField();
    }

    // Build an update-from-select query for the index entity main table and the
    // id-count query.
    String updateTableAlias = "updatetable";
    String tempTableAlias = "temptable";

    // Some rollupCounts source table are very large (eg. variant_to_person)
    // Split the read into smaller chunks: use Id field since the table may be clustered on it
    // If such splitting of the update is needed, add a where clause regex in rollupCounts.sql
    // format:  WHERE REGEXP_CONTAINS(id, r"{indexIdRegex}")
    String rollUpCountRawSql = rollupCountsBQTable.render();

    List<String> regexSubValues = List.of(StringUtils.EMPTY); // default: no-op
    if (rollUpCountRawSql.contains(INDEX_ID_REGEX_BRACES)) {
      regexSubValues = generateRegexIdSubValues();
    }

    regexSubValues.forEach(
        subValue -> {
          String rollupCountsTableSql =
              "SELECT "
                  + SqlQueryField.of(rollupCountsTableCountField).renderForSelect()
                  + ", "
                  + SqlQueryField.of(rollupCountsTableIdField).renderForSelect()
                  + " FROM "
                  + rollUpCountRawSql.replace(INDEX_ID_REGEX_BRACES, subValue);
          String updateFromSelectSql =
              "UPDATE "
                  + indexTable.getTablePointer().render()
                  + " AS "
                  + updateTableAlias
                  + " SET "
                  + SqlQueryField.of(entityTableCountField).renderForSelect(updateTableAlias)
                  + " = "
                  + SqlQueryField.of(rollupCountsTableCountField).renderForSelect(tempTableAlias)
                  + " FROM ("
                  + rollupCountsTableSql
                  + ") AS "
                  + tempTableAlias
                  + " WHERE "
                  + SqlQueryField.of(entityTableIdField).renderForSelect(updateTableAlias)
                  + " = "
                  + SqlQueryField.of(rollupCountsTableIdField).renderForSelect(tempTableAlias);
          LOGGER.info("update-from-select query: {}", updateFromSelectSql);

          // Run the update-from-select to write the count field in the index entity main table.
          runQueryIfTableExists(indexTable.getTablePointer(), updateFromSelectSql, isDryRun);
        });
  }

  private List<String> generateRegexIdSubValues() {
    // This is currently used only for variant_id and hence optimized for the same
    // If other entities need this, add regex values as a property of entityGroup config
    // i.e. property SZRollupCountsSql.regexIdSub &entityGroup.json#rollupCountsSql#regexIdSub
    Stream<String> digitStream =
        IntStream.range(1, 10)
            .mapToObj(
                i -> {
                  List<String> forI = new ArrayList<>();
                  // regex: 1 digit followed by alphabet
                  forI.add(String.format("^%d[^0-9]", i));
                  // regex: 1 digit followed by digit
                  forI.addAll(
                      IntStream.range(0, 10).mapToObj(j -> String.format("^%d%d", i, j)).toList());
                  return forI;
                })
            .flatMap(List::stream);

    Stream<String> alphabetStream =
        "XY"
            .chars()
            .mapToObj(
                c -> {
                  List<String> forC = new ArrayList<>();
                  // regex: 1 alphabet followed by digit
                  forC.add(String.format("^%c[0-9]", c));
                  // regex: 1 alphabet followed by alphabet
                  forC.add(String.format("^%c[^0-9]", c));
                  return forC;
                })
            .flatMap(List::stream);

    return Stream.concat(digitStream, alphabetStream).toList();
  }
}
