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
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

/**
 * A batch Apache Beam pipeline for precomputing counts and writing them to a new BQ table.
 *
 * <p>This pipeline counts the number of distinct auxiliary nodes for each primary node:
 *
 * <p>Primary = the nodes we want to generate counts for
 *
 * <p>Auxiliary = the nodes we want to count
 *
 * <p>For example, say we want to precompute the number of people who have at least one occurrence
 * of each condition. For each condition, we want to count the number of condition_occurrence rows,
 * grouped by the person. If one person has two occurrences of "diabetes", we only want to count
 * that once; This is the reason for the group by. This pipeline will output a table (condition, #
 * people with >=1 occurrence of condition). Using the definitions above:
 *
 * <p>Primary = Condition. We want to generate a count for each condition. Input is a list of
 * conditions.
 *
 * <p>Secondary = Condition_Occurrence. We want to count the number of people that have at least one
 * occurrence of each condition. Input is a list of (condition, person) pairs, one for each
 * occurrence.
 */
public final class PrecomputeCounts {
  private PrecomputeCounts() {}

  /** Options supported by {@link PrecomputeCounts}. */
  public interface PrecomputeCountsOptions extends BigQueryOptions {
    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve all primary entity instances. "
            + "The result of the query should have one column, (node).")
    String getAllPrimaryNodesQuery();

    void setAllPrimaryNodesQuery(String query);

    String getAllPrimaryNodesQueryText();

    void setAllPrimaryNodesQueryText(String query);

    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve all auxiliary entity instances. "
            + "The result of the query should have two columns, (node, secondary).")
    String getAllAuxiliaryNodesQuery();

    void setAllAuxiliaryNodesQuery(String query);

    String getAllAuxiliaryNodesQueryText();

    void setAllAuxiliaryNodesQueryText(String query);

    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve all ancestor-descendant relationships for the primary entity. "
            + "The result of the query should have two columns, (ancestor, descendant).")
    String getAncestorDescendantRelationshipsQuery();

    void setAncestorDescendantRelationshipsQuery(String query);

    String getAncestorDescendantRelationshipsQueryText();

    void setAncestorDescendantRelationshipsQueryText(String query);

    @Description(
        "The  \"[project_id]:[dataset_id].[table_id]\" specification of the precomputed-counts table to create.")
    String getOutputBigQueryTable();

    void setOutputBigQueryTable(String tableRef);

    @Description("What to name the node column on the output BigQuery table.")
    @Default.String("node")
    String getOutputNodeColumn();

    void setOutputNodeColumn(String nodeColumn);

    @Description("What to name the count column on the output BigQuery table.")
    @Default.String("count")
    String getOutputCountColumn();

    void setOutputCountColumn(String countColumn);
  }

  public static void main(String[] args) throws IOException {
    PrecomputeCountsOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(PrecomputeCountsOptions.class);
    Pipeline pipeline = Pipeline.create(options);

    // read in the queries from files
    String primaryNodesQuery = Files.readString(Path.of(options.getAllPrimaryNodesQuery()));
    String auxiliaryNodesQuery = Files.readString(Path.of(options.getAllAuxiliaryNodesQuery()));

    // read in the primary and auxiliary nodes from BQ
    PCollection<Long> primaryNodesPC =
        BigQueryUtils.readNodesFromBQ(pipeline, primaryNodesQuery, "primaryNodes");
    PCollection<KV<Long, Long>> auxiliaryNodesPC =
        BigQueryUtils.readAuxiliaryNodesFromBQ(pipeline, auxiliaryNodesQuery);

    // count the number of distinct auxiliary nodes per primary node
    PCollection<KV<Long, Long>> nodeCountKVsPC =
        CountUtils.countDistinct(primaryNodesPC, auxiliaryNodesPC);

    // optionally handle a hierarchy for the primary nodes
    boolean includesHierarchy = options.getAncestorDescendantRelationshipsQuery() != null;
    if (includesHierarchy) {
      // read in the ancestor-descendant relationships from BQ. build (descendant, ancestor) pairs
      String ancestorDescendantRelationshipsQuery =
          Files.readString(Path.of(options.getAncestorDescendantRelationshipsQuery()));
      PCollection<KV<Long, Long>> descendantAncestorKVsPC =
          BigQueryUtils.readAncestorDescendantRelationshipsFromBQ(
              pipeline, ancestorDescendantRelationshipsQuery);

      // aggregate the counts up the hierarchy
      nodeCountKVsPC =
          CountUtils.aggregateCountsInHierarchy(nodeCountKVsPC, descendantAncestorKVsPC);
    }

    // write the (node, count) rows to BQ
    writePrecomputedCountsToBQ(
        nodeCountKVsPC, options.getOutputBigQueryTable(), precomputedCountsSchema(options));

    pipeline.run().waitUntilFinish();
  }

  /** Write the list of (node, count) to BQ. */
  private static void writePrecomputedCountsToBQ(
      PCollection<KV<Long, Long>> nodeCountKVs, String outputBQTable, TableSchema outputBQSchema) {
    PCollection<TableRow> nodeCountBQRows =
        nodeCountKVs.apply(
            "build (node, count) pcollection of BQ rows",
            ParDo.of(
                new DoFn<KV<Long, Long>, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    KV<Long, Long> element = context.element();
                    PrecomputeCountsOptions contextOptions =
                        context.getPipelineOptions().as(PrecomputeCountsOptions.class);
                    context.output(
                        new TableRow()
                            .set(contextOptions.getOutputNodeColumn(), element.getKey())
                            .set(contextOptions.getOutputCountColumn(), element.getValue()));
                  }
                }));

    nodeCountBQRows.apply(
        "insert the (node, count) rows into BQ",
        BigQueryIO.writeTableRows()
            .to(outputBQTable)
            .withSchema(outputBQSchema)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  /** Defines the BigQuery schema for the output (node, count) table. */
  private static TableSchema precomputedCountsSchema(PrecomputeCountsOptions options) {
    return new TableSchema()
        .setFields(
            List.of(
                new TableFieldSchema()
                    .setName(options.getOutputNodeColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED"),
                new TableFieldSchema()
                    .setName(options.getOutputCountColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED")));
  }
}
