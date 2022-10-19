package bio.terra.tanagra.indexing.job;

import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.ANCESTOR_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.COUNT_ID_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.DESCENDANT_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.ID_COLUMN_NAME;
import static bio.terra.tanagra.indexing.job.beam.BigQueryUtils.ROLLUP_COUNT_COLUMN_NAME;

import bio.terra.tanagra.indexing.BigQueryIndexingJob;
import bio.terra.tanagra.indexing.job.beam.BigQueryUtils;
import bio.terra.tanagra.indexing.job.beam.CountUtils;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.underlay.Underlay;
import bio.terra.tanagra.underlay.entitygroup.CriteriaOccurrence;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A batch Apache Beam pipeline that counts the number of distinct occurrences for each primary
 * node, which may optionally include a hierarchy, and writes the results to a BQ table.
 */
public class ComputeRollupCounts extends BigQueryIndexingJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ComputeRollupCounts.class);

  // The default table schema for the id-rollupCount output table.
  private static final TableSchema ROLLUP_COUNT_TABLE_SCHEMA =
      new TableSchema()
          .setFields(
              List.of(
                  new TableFieldSchema()
                      .setName(ID_COLUMN_NAME)
                      .setType("INTEGER")
                      .setMode("REQUIRED"),
                  new TableFieldSchema()
                      .setName(ROLLUP_COUNT_COLUMN_NAME)
                      .setType("INTEGER")
                      .setMode("REQUIRED")));

  private final String hierarchyName;

  public ComputeRollupCounts(CriteriaOccurrence entityGroup) {
    this(entityGroup, null);
  }

  public ComputeRollupCounts(CriteriaOccurrence entityGroup, String hierarchyName) {
    super(entityGroup);
    this.hierarchyName = hierarchyName;
  }

  @Override
  public String getName() {
    return "COMPUTE ROLLUP COUNTS ("
        + getEntityGroup().getName()
        + ", "
        + (hierarchyName == null ? "NO HIERARCHY" : hierarchyName)
        + ")";
  }

  @Override
  public void run(boolean isDryRun) {
    CriteriaOccurrence entityGroup = (CriteriaOccurrence) getEntityGroup();

    String selectAllCriteriaIdsSql =
        entityGroup
            .getCriteriaEntity()
            .getMapping(Underlay.MappingType.SOURCE)
            .queryIds(ID_COLUMN_NAME)
            .renderSQL();
    LOGGER.info("select all criteria ids SQL: {}", selectAllCriteriaIdsSql);

    String selectAllCriteriaPrimaryIdPairsSql =
        entityGroup.queryCriteriaPrimaryPairs(ID_COLUMN_NAME, COUNT_ID_COLUMN_NAME).renderSQL();
    LOGGER.info(
        "select all criteria id - primary id pairs SQL: {}", selectAllCriteriaPrimaryIdPairsSql);

    String selectCriteriaAncestorDescendantIdPairsSql = null;
    if (hierarchyName != null) {
      selectCriteriaAncestorDescendantIdPairsSql =
          entityGroup
              .getCriteriaEntity()
              .getHierarchy(hierarchyName)
              .getMapping(Underlay.MappingType.INDEX)
              .queryAncestorDescendantPairs(ANCESTOR_COLUMN_NAME, DESCENDANT_COLUMN_NAME)
              .renderSQL();
    }
    LOGGER.info(
        "select all criteria ancestor - descendant id pairs SQL: {}",
        selectCriteriaAncestorDescendantIdPairsSql);

    Pipeline pipeline = Pipeline.create(buildDataflowPipelineOptions(getOutputDataPointer()));

    // read in the criteria ids and the criteria-primary id pairs from BQ
    PCollection<Long> criteriaIdsPC =
        BigQueryUtils.readNodesFromBQ(pipeline, selectAllCriteriaIdsSql, "criteriaIds");
    PCollection<KV<Long, Long>> occurrenceCriteriaIdPairsPC =
        BigQueryUtils.readOccurrencesFromBQ(pipeline, selectAllCriteriaPrimaryIdPairsSql);

    // optionally handle a hierarchy for the criteria entity
    if (hierarchyName != null) {
      // read in the ancestor-descendant relationships from BQ. build (descendant, ancestor) pairs
      PCollection<KV<Long, Long>> descendantAncestorKVsPC =
          BigQueryUtils.readAncestorDescendantRelationshipsFromBQ(
              pipeline, selectCriteriaAncestorDescendantIdPairsSql);

      // expand the set of occurrences to include a repeat for each ancestor
      occurrenceCriteriaIdPairsPC =
          CountUtils.repeatOccurrencesForHierarchy(
              occurrenceCriteriaIdPairsPC, descendantAncestorKVsPC);
    }

    // count the number of distinct occurrences per primary node
    PCollection<KV<Long, Long>> nodeCountKVsPC =
        CountUtils.countDistinct(criteriaIdsPC, occurrenceCriteriaIdPairsPC);

    // write the (id, count) rows to BQ
    writeCountsToBQ(nodeCountKVsPC, getOutputTablePointer());

    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }

  @Override
  @VisibleForTesting
  public TablePointer getOutputTablePointer() {
    return ((CriteriaOccurrence) getEntityGroup())
        .getCriteriaPrimaryRollupAuxiliaryData()
        .getMapping(Underlay.MappingType.INDEX)
        .getTablePointer();
  }

  /** Write the {@link KV} pairs (id, rollup_count) to BQ. */
  private static void writeCountsToBQ(
      PCollection<KV<Long, Long>> nodeCountKVs, TablePointer outputBQTable) {
    PCollection<TableRow> nodeCountBQRows =
        nodeCountKVs.apply(
            "build (id, rollup_count) pcollection of BQ rows",
            ParDo.of(
                new DoFn<KV<Long, Long>, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    KV<Long, Long> element = context.element();
                    context.output(
                        new TableRow()
                            .set(ID_COLUMN_NAME, element.getKey())
                            .set(ROLLUP_COUNT_COLUMN_NAME, element.getValue()));
                  }
                }));

    nodeCountBQRows.apply(
        "insert the (id, rollup_count) rows into BQ",
        BigQueryIO.writeTableRows()
            .to(outputBQTable.getPathForIndexing())
            .withSchema(ROLLUP_COUNT_TABLE_SCHEMA)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }
}
