package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.query.bigquery.BigQueryDataset;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import com.google.cloud.bigquery.Clustering;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableId;
import java.util.List;
import java.util.stream.Collectors;

public class CreateEntityMain extends BigQueryJob {
  private final Entity entity;
  private final ITEntityMain indexTable;

  public CreateEntityMain(SZIndexer indexerConfig, Entity entity, ITEntityMain indexTable) {
    super(indexerConfig);
    this.entity = entity;
    this.indexTable = indexTable;
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Build the column schemas.
    List<Field> fields =
        indexTable.getColumnSchemas().stream()
            .map(
                columnSchema ->
                    Field.newBuilder(
                            columnSchema.getColumnName(),
                            BigQueryDataset.fromSqlDataType(columnSchema.getSqlDataType()))
                        .build())
            .collect(Collectors.toList());

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
        clusterFields.isEmpty() ? null : Clustering.newBuilder().setFields(clusterFields).build();

    // Create an empty table with this schema.
    TableId destinationTable =
        TableId.of(
            indexerConfig.bigQuery.indexData.projectId,
            indexerConfig.bigQuery.indexData.datasetId,
            getOutputTableName());
    bigQueryExecutor
        .getBigQueryService()
        .createTableFromSchema(destinationTable, Schema.of(fields), clustering, isDryRun);
  }
}
