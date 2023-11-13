package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.indexing.job.dataflow.beam.BigQueryBeamUtils;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STEntityAttributes;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidateDataTypes extends BigQueryJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidateDataTypes.class);

  private final Entity entity;
  private final STEntityAttributes sourceTable;
  private final ITEntityMain indexTable;

  public ValidateDataTypes(
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
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return outputTableHasAtLeastOneRow() ? JobStatus.COMPLETE : JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Build the query to select all the attributes from the source table.
    Query selectAttributesFromSourceTable = sourceTable.getQueryAll(Map.of());
    LOGGER.info("Generated select SQL: {}", selectAttributesFromSourceTable.renderSQL());

    Schema sourceQueryResultSchema =
        bigQueryExecutor
            .getBigQueryService()
            .getQuerySchemaWithCaching(selectAttributesFromSourceTable.renderSQL());
    LOGGER.info("Select SQL results schema: {}", sourceQueryResultSchema);

    // Check that the schema data types match those of the index table columns.
    boolean foundError = false;
    for (Attribute attribute : entity.getAttributes()) {
      ColumnSchema sourceTableSchema = sourceTable.getAttributeValueColumnSchema(attribute);
      LegacySQLTypeName sourceTableBQDataType =
          BigQueryBeamUtils.fromSqlDataType(sourceTableSchema.getSqlDataType());

      Field sourceQueryField =
          sourceQueryResultSchema.getFields().get(sourceTableSchema.getColumnName());
      boolean dataTypesMatch = sourceTableBQDataType.equals(sourceQueryField.getType());
      if (!dataTypesMatch) {
        foundError = true;
        LOGGER.info(
            "Data type mismatch found for attribute {}: entity declared {}, SQL schema returns {}",
            attribute.getName(),
            sourceTableBQDataType,
            sourceQueryField.getType());
      }
    }
    if (foundError) {
      throw new InvalidConfigException("Data type mismatch found for entity: " + entity.getName());
    }
  }

  @Override
  public void clean(boolean isDryRun) {
    LOGGER.info(
        "Nothing to clean. This job only validates the entity table schema. It does not write any index data.");
  }
}
