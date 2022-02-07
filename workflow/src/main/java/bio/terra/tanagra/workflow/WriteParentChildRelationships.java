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
 * A batch Apache Beam pipeline for building a table that contains all parent-child relationships
 * for a hierarchical entity.
 */
public final class WriteParentChildRelationships {
  private WriteParentChildRelationships() {}

  /** Options supported by {@link WriteParentChildRelationships}. */
  public interface WriteParentChildRelationshipsOptions extends BigQueryOptions {
    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve all (parent, child) "
            + "relationships between entity instances. The result of the query should have "
            + "two columns, (parent,child).")
    String getParentChildQuery();

    void setParentChildQuery(String query);

    @Description(
        "The  \"[project_id]:[dataset_id].[table_id]\" specification of the parent-child table to create.")
    String getOutputBigQueryTable();

    void setOutputBigQueryTable(String tableRef);

    @Description("What to name the parent column on the output BigQuery table.")
    @Default.String("parent")
    String getOutputParentColumn();

    void setOutputParentColumn(String parentColumn);

    @Description("What to name the child column on the output BigQuery table.")
    @Default.String("child")
    String getOutputChildColumn();

    void setOutputChildColumn(String childColumn);
  }

  public static void main(String[] args) throws IOException {
    WriteParentChildRelationshipsOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(WriteParentChildRelationshipsOptions.class);
    Pipeline pipeline = Pipeline.create(options);

    // read in the query from file
    String parentChildQuery = Files.readString(Path.of(options.getParentChildQuery()));

    // read in the nodes from BQ
    PCollection<KV<Long, Long>> childParentRelationshipsPC =
        BigQueryUtils.readChildParentRelationshipsFromBQ(pipeline, parentChildQuery);

    // write the (parent,child) rows to BQ
    writeParentChildRelationshipsToBQ(
        childParentRelationshipsPC, options.getOutputBigQueryTable(), parentChildSchema(options));

    pipeline.run().waitUntilFinish();
  }

  /** Write the list of {@link KV} (parent, child) pairs to BQ. */
  private static void writeParentChildRelationshipsToBQ(
      PCollection<KV<Long, Long>> childParentKVs,
      String outputBQTable,
      TableSchema outputBQSchema) {
    PCollection<TableRow> childParentBQRows =
        childParentKVs.apply(
            "build (parent, child) pcollection of BQ rows",
            ParDo.of(
                new DoFn<KV<Long, Long>, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    KV<Long, Long> element = context.element();
                    WriteParentChildRelationshipsOptions contextOptions =
                        context.getPipelineOptions().as(WriteParentChildRelationshipsOptions.class);
                    context.output(
                        new TableRow()
                            .set(contextOptions.getOutputParentColumn(), element.getValue())
                            .set(contextOptions.getOutputChildColumn(), element.getKey()));
                  }
                }));

    childParentBQRows.apply(
        "insert the (parent, child) rows into BQ",
        BigQueryIO.writeTableRows()
            .to(outputBQTable)
            .withSchema(outputBQSchema)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  /** Defines the BigQuery schema for the output (parent, child) table. */
  private static TableSchema parentChildSchema(WriteParentChildRelationshipsOptions options) {
    return new TableSchema()
        .setFields(
            List.of(
                new TableFieldSchema()
                    .setName(options.getOutputParentColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED"),
                new TableFieldSchema()
                    .setName(options.getOutputChildColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED")));
  }
}
