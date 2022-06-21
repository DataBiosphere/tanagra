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
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;

/** A batch Apache Beam pipeline for building a table that contains all entity instances. */
public final class WriteAllNodes {
  private WriteAllNodes() {}

  /** Options supported by {@link WriteAllNodes}. */
  public interface WriteAllNodesOptions extends RunUnderlayWorkflowsOptions {
    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve all entity instances. "
            + "The result of the query should have one column, (node).")
    String getAllNodesQuery();

    void setAllNodesQuery(String query);

    @Description(
        "A BigQuery standard SQL query to execute to retrieve all entity instances. "
            + "The result of the query should have one column, (node).")
    String getAllNodesQueryText();

    void setAllNodesQueryText(String query);

    @Description(
        "The  \"[project_id]:[dataset_id].[table_id]\" specification of the all-nodes table to create.")
    String getOutputBigQueryTable();

    void setOutputBigQueryTable(String tableRef);

    @Description("What to name the node column on the output BigQuery table.")
    @Default.String("node")
    String getOutputNodeColumn();

    void setOutputNodeColumn(String nodeColumn);
  }

  public static void main(String[] args) throws IOException {
    WriteAllNodesOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(WriteAllNodesOptions.class);

    // read in the query from file
    if (options.getAllNodesQueryText() == null) {
      options.setAllNodesQueryText(Files.readString(Path.of(options.getAllNodesQuery())));
    }

    run(options);
  }

  public static void run(WriteAllNodesOptions options) {
    Pipeline pipeline = Pipeline.create(options);

    // read in the nodes from BQ
    PCollection<Long> allNodesPC =
        BigQueryUtils.readNodesFromBQ(pipeline, options.getAllNodesQueryText(), "allNodes");

    // write the (node) rows to BQ
    writeAllNodesToBQ(allNodesPC, options.getOutputBigQueryTable(), allNodesSchema(options));

    pipeline.run().waitUntilFinish();
  }

  /** Write the list of (node) to BQ. */
  private static void writeAllNodesToBQ(
      PCollection<Long> allNodeKVs, String outputBQTable, TableSchema outputBQSchema) {
    PCollection<TableRow> allNodeBQRows =
        allNodeKVs.apply(
            "build (node) pcollection of BQ rows",
            ParDo.of(
                new DoFn<Long, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    Long element = context.element();
                    WriteAllNodesOptions contextOptions =
                        context.getPipelineOptions().as(WriteAllNodesOptions.class);
                    context.output(
                        new TableRow().set(contextOptions.getOutputNodeColumn(), element));
                  }
                }));

    allNodeBQRows.apply(
        "insert the (node) rows into BQ",
        BigQueryIO.writeTableRows()
            .to(outputBQTable)
            .withSchema(outputBQSchema)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  /** Defines the BigQuery schema for the output (node) table. */
  private static TableSchema allNodesSchema(WriteAllNodesOptions options) {
    return new TableSchema()
        .setFields(
            List.of(
                new TableFieldSchema()
                    .setName(options.getOutputNodeColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED")));
  }
}
