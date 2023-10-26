package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.bigquery.BigQueryDataset;
import bio.terra.tanagra.underlay2.indextable.ITEntityMain;
import bio.terra.tanagra.underlay2.serialization.SZIndexer;
import com.google.cloud.bigquery.Clustering;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreateEntityTable extends BigQueryJob {
  private final ITEntityMain indexTable;

  public CreateEntityTable(SZIndexer indexerConfig, ITEntityMain indexTable) {
    super(indexerConfig);
    this.indexTable = indexTable;
  }

  @Override
  public String getName() {
    return String.format("%s-%s", this.getClass().getSimpleName(), indexTable.getEntity());
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public void run(boolean isDryRun) {
    // Build field schemas for entity attributes.
    List<Field> fields = new ArrayList<>();
    getEntity().getAttributes().stream()
        .forEach(
            attribute -> {
              AttributeMapping indexMapping = attribute.getMapping(Underlay.MappingType.INDEX);
              AttributeMapping sourceMapping = attribute.getMapping(Underlay.MappingType.SOURCE);
              indexMapping.buildColumnSchemasForIndexing(sourceMapping).stream()
                  .forEach(columnSchema -> fields.add(fromColumnSchema(columnSchema)));
            });

    // Build field schemas for text mapping.
    if (getEntity().getTextSearch().isEnabled()) {
      TextSearchMapping textSearchMapping =
          getEntity().getTextSearch().getMapping(Underlay.MappingType.INDEX);
      if (textSearchMapping.definedBySearchString()
          && textSearchMapping.getTablePointer().equals(getEntityIndexTable())) {
        fields.add(
            fromColumnSchema(
                new ColumnSchema(
                    textSearchMapping.getSearchString().getColumnName(),
                    CellValue.SQLDataType.STRING)));
      }
    }

    // Build field schemas for hierarchy fields: path, num_children.
    // The other two hierarchy fields, is_root and is_member, are calculated from path.
    getEntity().getHierarchies().stream()
        .forEach(
            hierarchy -> {
              fields.add(
                  fromColumnSchema(
                      hierarchy.getField(HierarchyField.Type.PATH).buildColumnSchema()));
              fields.add(
                  fromColumnSchema(
                      hierarchy.getField(HierarchyField.Type.NUM_CHILDREN).buildColumnSchema()));
            });

    // Build field schemas for relationship fields: count, display_hints.
    getEntity().getRelationships().stream()
        .forEach(
            relationship ->
                relationship.getFields().stream()
                    .filter(relationshipField -> relationshipField.getEntity().equals(getEntity()))
                    .forEach(
                        relationshipField ->
                            fields.add(fromColumnSchema(relationshipField.buildColumnSchema()))));

    // Build a clustering specification.
    List<String> clusterFields;
    if (!getEntity().getFrequentFilterAttributes().isEmpty()) {
      // If the frequent filter attributes are defined in the config, use those.
      clusterFields =
          getEntity().getFrequentFilterAttributes().stream()
              .map(a -> a.getMapping(Underlay.MappingType.INDEX).getValue().getColumnName())
              .collect(Collectors.toList());
    } else if (getEntity().getTextSearch().isEnabled()) {
      // If not, the use the text search string, if there is one.
      clusterFields =
          List.of(
              getEntity()
                  .getTextSearch()
                  .getMapping(Underlay.MappingType.INDEX)
                  .getSearchString()
                  .getColumnName());
    } else {
      // Otherwise skip clustering.
      clusterFields = List.of();
    }
    Clustering clustering =
        clusterFields.isEmpty() ? null : Clustering.newBuilder().setFields(clusterFields).build();

    // Create an empty table with this schema.
    BigQueryDataset outputBQDataset = getBQDataPointer(getEntityIndexTable());
    TableId destinationTable =
        TableId.of(
            outputBQDataset.getProjectId(),
            outputBQDataset.getDatasetId(),
            getEntityIndexTable().getTableName());
    outputBQDataset
        .getBigQueryService()
        .createTableFromSchema(destinationTable, Schema.of(fields), clustering, isDryRun);
  }

  private static Field fromColumnSchema(ColumnSchema columnSchema) {
    Field.Builder field =
        Field.newBuilder(
            columnSchema.getColumnName(),
            BigQueryDataset.fromSqlDataType(columnSchema.getSqlDataType()));

    return field.build();
  }

  @Override
  public JobStatus checkStatus() {
    return getOutputTable().isPresent() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }
}
