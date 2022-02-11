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
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;

/**
 * A batch Apache Beam pipeline for building a table that contains all text search information for
 * an entity.
 */
public final class BuildTextSearchInformation {
  private BuildTextSearchInformation() {}

  /** Options supported by {@link BuildTextSearchInformation}. */
  public interface BuildTextSearchInformationOptions extends BigQueryOptions {
    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve all entity instances. "
            + "The result of the query should have one column, (node).")
    String getAllNodesQuery();

    void setAllNodesQuery(String query);

    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve all search strings for entity"
            + " instances. The result of the query should have two columns, (node,text).")
    String getSearchStringsQuery();

    void setSearchStringsQuery(String query);

    @Description(
        "The  \"[project_id]:[dataset_id].[table_id]\" specification of the search text information"
            + " table to create.")
    String getOutputBigQueryTable();

    void setOutputBigQueryTable(String tableRef);

    @Description("What to name the node column on the output BigQuery table.")
    @Default.String("node")
    String getOutputNodeColumn();

    void setOutputNodeColumn(String nodeColumn);

    @Description("What to name the search text column on the output BigQuery table.")
    @Default.String("text")
    String getOutputTextColumn();

    void setOutputTextColumn(String textColumn);
  }

  public static void main(String[] args) throws IOException {
    BuildTextSearchInformationOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(BuildTextSearchInformationOptions.class);
    Pipeline pipeline = Pipeline.create(options);

    // read in the queries from file
    String searchStringsQuery = Files.readString(Path.of(options.getSearchStringsQuery()));
    String allNodesQuery = Files.readString(Path.of(options.getAllNodesQuery()));

    // read in the nodes from BQ
    PCollection<Long> allNodesPC =
        BigQueryUtils.readNodesFromBQ(pipeline, allNodesQuery, "allNodes");

    // read in the search strings from BQ
    PCollection<KV<Long, String>> searchStringsPC =
        readSearchStringsFromBQ(pipeline, searchStringsQuery);

    // concatenate all the search strings for a particular entity instance into a single string
    PCollection<KV<Long, String>> concatenatedSearchStringsPC =
        TextSearchUtils.concatenateSearchStringsByKey(allNodesPC, searchStringsPC);

    // write the (node,text) rows to BQ
    writeSearchStringsToBQ(
        concatenatedSearchStringsPC, options.getOutputBigQueryTable(), nodeTextSchema(options));

    pipeline.run().waitUntilFinish();
  }

  /**
   * Read all the search strings from BQ and build a {@link PCollection} of {@link KV} pairs (node,
   * text).
   */
  public static PCollection<KV<Long, String>> readSearchStringsFromBQ(
      Pipeline pipeline, String sqlQuery) {
    PCollection<TableRow> searchStringBqRows =
        pipeline.apply(
            "read all (node, text) rows",
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return searchStringBqRows.apply(
        "build (node, text) pcollection",
        MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.strings()))
            .via(
                tableRow -> {
                  Long node = Long.parseLong((String) tableRow.get("node"));
                  String text = (String) tableRow.get("text");
                  return KV.of(node, text);
                }));
  }

  /** Write the list of {@link KV} (node, text) pairs to BQ. */
  private static void writeSearchStringsToBQ(
      PCollection<KV<Long, String>> searchStringKVs,
      String outputBQTable,
      TableSchema outputBQSchema) {
    PCollection<TableRow> searchStringBQRows =
        searchStringKVs.apply(
            "build (node, text) pcollection of BQ rows",
            ParDo.of(
                new DoFn<KV<Long, String>, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) {
                    KV<Long, String> element = context.element();
                    BuildTextSearchInformationOptions contextOptions =
                        context.getPipelineOptions().as(BuildTextSearchInformationOptions.class);
                    context.output(
                        new TableRow()
                            .set(contextOptions.getOutputNodeColumn(), element.getKey())
                            .set(contextOptions.getOutputTextColumn(), element.getValue()));
                  }
                }));

    searchStringBQRows.apply(
        "insert the (node, text) rows into BQ",
        BigQueryIO.writeTableRows()
            .to(outputBQTable)
            .withSchema(outputBQSchema)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));
  }

  /** Defines the BigQuery schema for the output (node, text) table. */
  private static TableSchema nodeTextSchema(BuildTextSearchInformationOptions options) {
    return new TableSchema()
        .setFields(
            List.of(
                new TableFieldSchema()
                    .setName(options.getOutputNodeColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED"),
                new TableFieldSchema()
                    .setName(options.getOutputTextColumn())
                    .setType("STRING")
                    .setMode("NULLABLE")));
  }
}
