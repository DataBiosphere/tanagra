package bio.terra.tanagra.workflow;

import bio.terra.tanagra.proto.underlay.Column;
import bio.terra.tanagra.proto.underlay.Dataset;
import bio.terra.tanagra.proto.underlay.Table;
import bio.terra.tanagra.proto.underlay.Underlay;
import bio.terra.tanagra.workflow.FlattenHierarchy.FlattenHierarchyOptions;
import com.google.api.services.bigquery.model.TableReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.TypedRead.Method;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;

// DO NOT SUBMIT comment me
public class CopyBqTable {
  private CopyBqTable() {}

  /** Options supported by {@link CopyBqTable}. */
  public interface CopyBqTableOptions extends BigQueryOptions {
    @Description(
        "Path to a BigQuery standard SQL query file to execute to retrieve the hierarchy to be"
            + "flattened. The result of the query should have two columns, (parent, child) that"
            + "defines all of the direct relationships of the hierarchy to be flattened.")
    String getHierarchyQuery();

    void setHierarchyQuery(String query);

    @Description(
        "The maximum depth of ancestors present than the hierarchy. This may be larger "
            + "than the actual max depth, but if it is smaller the resulting table will be incomplete")
    @Default.Integer(64)
    int getMaxHierarchyDepth();

    void setMaxHierarchyDepth(int val);

    @Description(
        "The  \"[project_id]:[dataset_id].[table_id]\" specification of the ancestor table to create.")
    String getOutputBigQueryTable();

    void setOutputBigQueryTable(String tableRef);

    @Description("What to name the ancestor column on the output BigQuery table.")
    @Default.String("ancestor")
    String getOutputAncestorColumn();

    void setOutputAncestorColumn(String ancestorColumn);

    @Description("What to name the descendant column on the output BigQuery table.")
    @Default.String("descendant")
    String getOutputDescendantColumn();

    void setOutputDescendantColumn(String descendantColumn);
  }


  public static void main(String[] args) {
    CopyBqTableOptions options =
        PipelineOptionsFactory
            .fromArgs(args).withValidation().as(CopyBqTable.CopyBqTableOptions.class);

    Underlay underlay = Underlay.newBuilder().build(); // do not submit load me
    Dataset dataset = underlay.getDatasetsList().get(0);
    Table table = dataset.getTables(0);

    TableReference tableReference = new TableReference().setProjectId(dataset.getBigQueryDataset().getProjectId())
        .setDatasetId(dataset.getBigQueryDataset().getProjectId())
        .setTableId(table.getName());


    Pipeline pipeline = Pipeline.create(options);

    pipeline.apply(
        BigQueryIO.readTableRows()
            .from(tableReference)
            .withMethod(Method.DIRECT_READ)
            .withSelectedFields(table.getColumnsList().stream().map(Column::getName).collect(
                Collectors.toList())));
  }
}
