package bio.terra.tanagra.indexing.job.dataflow;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.DataflowUtils;
import bio.terra.tanagra.indexing.job.dataflow.beam.DuplicateHandlingUtils;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STEntityAttributes;
import com.google.api.services.bigquery.model.Clustering;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A batch Apache Beam pipeline for processing entity instances and writing them to a new table. */
public class WriteEntityMain extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(WriteEntityMain.class);
  private final Entity entity;
  private final STEntityAttributes sourceTable;
  private final ITEntityMain indexTable;

  public WriteEntityMain(
      SZIndexer indexerConfig,
      Entity entity,
      STEntityAttributes sourceTable,
      ITEntityMain indexTable) {
    super(indexerConfig);
    this.entity = entity;
    this.sourceTable = sourceTable;
    this.indexTable = indexTable;
  }

  @Override
  public String getEntity() {
    return entity.getName();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return outputTableHasAtLeastOneRow() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Build the pipeline object from the Dataflow config.
    Pipeline pipeline = Pipeline.create(DataflowUtils.getPipelineOptions(indexerConfig, getName()));

    // Build the source query and the pipeline steps to read the results.
    String selectAllSourceRowsSql = "SELECT * FROM " + sourceTable.getTablePointer().render();
    LOGGER.info("Select all source rows SQL: {}", selectAllSourceRowsSql);
    String sourceIdColumn =
        sourceTable.getAttributeValueColumnSchema(entity.getIdAttribute()).getColumnName();
    PCollection<TableRow> allRows =
        pipeline.apply(
            "read all-source-rows query result",
            BigQueryIO.readTableRows()
                .fromQuery(selectAllSourceRowsSql)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());

    // Map each row to the id.
    PCollection<KV<Long, TableRow>> allRowsKeyedById =
        allRows.apply(
            MapElements.into(
                    TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptor.of(TableRow.class)))
                .via(
                    tableRow ->
                        KV.of(Long.parseLong((String) tableRow.get(sourceIdColumn)), tableRow)));

    // Remove duplicates if requested.
    PCollection<TableRow> allRowsFiltered =
        DuplicateHandlingUtils.run(entity.getDuplicateHandling(), allRowsKeyedById);

    // Build the schema for the output table.
    List<TableFieldSchema> outputFieldSchemas =
        indexTable.getColumnSchemas().stream()
            .map(
                columnSchema ->
                    new TableFieldSchema()
                        .setName(columnSchema.getColumnName())
                        .setType(BigQueryBeamUtils.fromDataType(columnSchema.getDataType()).name())
                        .setMode(columnSchema.isRequired() ? "REQUIRED" : "NULLABLE"))
            .collect(Collectors.toList());
    TableSchema outputTableSchema = new TableSchema().setFields(outputFieldSchemas);

    // Build a clustering specification.
    List<String> clusterFields;
    if (!entity.getOptimizeGroupByAttributes().isEmpty()) {
      // If the optimize group by attributes are defined, use those.
      clusterFields =
          entity.getOptimizeGroupByAttributes().stream()
              .map(
                  attribute ->
                      indexTable.getAttributeValueField(attribute.getName()).getColumnName())
              .collect(Collectors.toList());
    } else if (entity.hasTextSearch()) {
      // If not, and the text search field is defined, use that.
      clusterFields = List.of(indexTable.getTextSearchField().getColumnName());
    } else {
      // Otherwise skip clustering.
      clusterFields = List.of();
    }
    Clustering clustering =
        new Clustering().setFields(clusterFields.isEmpty() ? null : clusterFields);

    // Build the output-to-bigquery step into the pipeline.
    allRowsFiltered.apply(
        BigQueryIO.writeTableRows()
            .to(
                BigQueryBeamUtils.getTableSqlPath(
                    indexerConfig.bigQuery.indexData.projectId,
                    indexerConfig.bigQuery.indexData.datasetId,
                    indexTable.getTablePointer().getTableName()))
            .withSchema(outputTableSchema)
            .withClustering(clustering)
            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_EMPTY)
            .withMethod(BigQueryIO.Write.Method.FILE_LOADS));

    // Kick off the pipeline.
    if (!isDryRun) {
      pipeline.run().waitUntilFinish();
    }
  }
}
