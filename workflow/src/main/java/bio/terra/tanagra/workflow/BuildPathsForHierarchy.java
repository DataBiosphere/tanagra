package bio.terra.tanagra.workflow;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Distinct;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;

/**
 * A batch Apache Beam pipeline for building a table that contains a path (i.e. a list of ancestors in order) for each node in a hierarchy.
 */
public final class BuildPathsForHierarchy {
  private BuildPathsForHierarchy() {}

  /** Options supported by {@link FlattenHierarchy}. */
  public interface BuildPathsForHierarchyOptions extends BigQueryOptions {
    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve all nodes in the hierarchy. "
            + "The result of the query should have one column, (node) that defines a key we can use to"
            + "lookup the parent of that node.")
    String getAllNodesQuery();

    void setAllNodesQuery(String query);

    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve the parent of a node in the "
            + "hierarchy. The result of the query should have one column, (parent) that defines the "
            + "key used to lookup the parent of that node. The query must include the named parameter @node, "
            + "so we can lookup the parent of an arbitrary node.")
    String getParentNodeQuery();

    void setParentNodeQuery(String query);

    @Description(
        "The maximum depth of ancestors present in the hierarchy. This may be larger "
            + "than the actual max depth, but if it is smaller the resulting table will be incomplete")
    @Default.Integer(64)
    int getMaxHierarchyDepth();

    void setMaxHierarchyDepth(int val);

    @Description(
        "The  \"[project_id]:[dataset_id].[table_id]\" specification of the node-path table to create.")
    String getOutputBigQueryTable();

    void setOutputBigQueryTable(String tableRef);

    @Description("What to name the node column on the output BigQuery table.")
    @Default.String("node")
    String getOutputNodeColumn();

    void setOutputNodeColumn(String nodeColumn);

    @Description("What to name the path column on the output BigQuery table.")
    @Default.String("path")
    String getOutputPathColumn();

    void setOutputPathColumn(String pathColumn);
  }

  /** Parses a row containing a (node) long value. */
  private static Long nodeRowToLong(TableRow row) {
    return Long.parseLong((String) row.get("node"));
  }

  /** Defines the BigQuery schema for the output hierarchy table. */
  private static TableSchema pathsForHierarchySchema(BuildPathsForHierarchyOptions options) {
    return new TableSchema()
        .setFields(
            List.of(
                new TableFieldSchema()
                    .setName(options.getOutputNodeColumn())
                    .setType("INTEGER")
                    .setMode("REQUIRED"),
                new TableFieldSchema()
                    .setName(options.getOutputPathColumn())
                    .setType("INTEGER")
                    .setMode("REPEATED")));
  }

  public static void main(String[] args) throws IOException {
    BuildPathsForHierarchyOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(BuildPathsForHierarchyOptions.class);

    String allNodesQuery = Files.readString(Path.of(options.getAllNodesQuery()));
    String getParentNodeQuery = Files.readString(Path.of(options.getParentNodeQuery()));

    Pipeline pipeline = Pipeline.create(options);

    // build a collection of all the nodes
    PCollection<Long> allNodes =
        pipeline
            .apply(
                "read all nodes query",
                BigQueryIO.readTableRows()
                    .fromQuery(allNodesQuery)
                    .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                    .usingStandardSql())
            .apply(
                MapElements.into(TypeDescriptors.longs())
                    .via(BuildPathsForHierarchy::nodeRowToLong))
            .apply(Distinct.create());

    // for each node, recursively run the get-parent query until there are no more parents.
    // build up a list of parents in order and construct a BQ row object (node, path).
    PCollection<TableRow> allNodesAndPathsBQRows =
        allNodes.apply(
            ParDo.of(
                new DoFn<Long, TableRow>() {
                  @ProcessElement
                  public void processElement(ProcessContext context) throws InterruptedException {
                    BuildPathsForHierarchyOptions contextOptions =
                        context.getPipelineOptions().as(BuildPathsForHierarchyOptions.class);
                    Long node = context.element();

                    // get a BQ client object that uses application-default credentials
                    BigQuery bigQueryClient =
                        com.google.cloud.bigquery.BigQueryOptions.newBuilder()
                            .setProjectId(contextOptions.getBigQueryProject())
                            .build()
                            .getService();

                    // recursively lookup the parent node id. if there are >1 parent nodes, choose
                    // the first one returned. recurse a maximum of MaxHierarchyDepth times.
                    List<Long> path = new ArrayList<>();
                    Long childNode = node;
                    int ctr = 0;
                    while (ctr < contextOptions.getMaxHierarchyDepth()) {

                      // run the get-parent-node query against the BQ dataset
                      QueryJobConfiguration queryConfig =
                          QueryJobConfiguration.newBuilder(getParentNodeQuery)
                              .addNamedParameter(
                                  "node",
                                  QueryParameterValue.newBuilder()
                                      .setType(StandardSQLTypeName.INT64)
                                      .setValue(childNode.toString())
                                      .build())
                              .build();
                      TableResult results = bigQueryClient.query(queryConfig);

                      // add the parent node to the path = list of ancestors in order
                      try {
                        Long parentNode =
                            results.iterateAll().iterator().next().get("parent").getLongValue();
                        path.add(parentNode);
                        childNode = parentNode;
                        ctr++;
                      } catch (NoSuchElementException nseEx) {
                        break;
                      }
                    }

                    // build the BQ row object to insert
                    TableRow row =
                        new TableRow()
                            .set(contextOptions.getOutputNodeColumn(), node)
                            .set(contextOptions.getOutputPathColumn(), path);
                    context.output(row);
                  }
                }));

    // insert all these rows into BQ
    allNodesAndPathsBQRows.apply(
        BigQueryIO.writeTableRows()
            .to(options.getOutputBigQueryTable())
            .withSchema(pathsForHierarchySchema(options))
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));

    pipeline.run().waitUntilFinish();
  }
}
