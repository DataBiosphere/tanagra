package bio.terra.tanagra.indexing.job.bigquery;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.indexing.job.BigQueryJob;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.entitymodel.Entity;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import bio.terra.tanagra.underlay.serialization.SZIndexer;
import bio.terra.tanagra.underlay.sourcetable.STEntityAttributes;
import com.google.cloud.StringEnumValue;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.JobStatistics;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import java.util.Set;
import java.util.stream.Collectors;
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
  public String getEntity() {
    return entity.getName();
  }

  @Override
  protected String getOutputTableName() {
    return indexTable.getTablePointer().getTableName();
  }

  @Override
  public JobStatus checkStatus() {
    return JobStatus.NOT_STARTED;
  }

  @Override
  public void run(boolean isDryRun) {
    // Build the query to select all the attributes from the source table.
    String selectOneRowSql = "SELECT * FROM " + sourceTable.getTablePointer().render();
    LOGGER.info("Generated select SQL: {}", selectOneRowSql);

    // Dry run the query just to get the returned schema.
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(selectOneRowSql)
            .setUseLegacySql(false)
            .setDryRun(true)
            .build();
    JobStatistics.QueryStatistics queryStatistics = googleBigQuery.queryStatistics(queryConfig);
    Schema sourceQueryResultSchema = queryStatistics.getSchema();
    LOGGER.info("Select SQL results schema: {}", sourceQueryResultSchema);

    // Check that the schema data types match those of the index table columns.
    boolean foundError = false;
    for (Attribute attribute : entity.getAttributes()) {
      ColumnSchema sourceTableSchema = sourceTable.getAttributeValueColumnSchema(attribute);
      Set<LegacySQLTypeName> sourceTableBQDataTypes;
      switch (sourceTableSchema.getDataType()) {
        case STRING:
          sourceTableBQDataTypes = Set.of(LegacySQLTypeName.STRING);
          break;
        case INT64:
          sourceTableBQDataTypes = Set.of(LegacySQLTypeName.INTEGER);
          break;
        case BOOLEAN:
          sourceTableBQDataTypes = Set.of(LegacySQLTypeName.BOOLEAN);
          break;
        case DATE:
          sourceTableBQDataTypes = Set.of(LegacySQLTypeName.DATE);
          break;
        case DOUBLE:
          sourceTableBQDataTypes = Set.of(LegacySQLTypeName.NUMERIC, LegacySQLTypeName.FLOAT);
          break;
        case TIMESTAMP:
          sourceTableBQDataTypes = Set.of(LegacySQLTypeName.TIMESTAMP);
          break;
        default:
          throw new SystemException(
              "SQL data type not supported for BigQuery: " + sourceTableSchema.getDataType());
      }
      Field sourceQueryField =
          sourceQueryResultSchema.getFields().get(sourceTableSchema.getColumnName());
      boolean dataTypesMatch = sourceTableBQDataTypes.contains(sourceQueryField.getType());
      if (!dataTypesMatch) {
        foundError = true;
        LOGGER.info(
            "Data type mismatch found for attribute {}: entity declared {}, SQL schema returns {}",
            attribute.getName(),
            sourceTableBQDataTypes.stream()
                .map(StringEnumValue::name)
                .collect(Collectors.joining(",", "[", "]")),
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

  @Override
  public boolean checkStatusAfterRunMatchesExpected(
      RunType runType, boolean isDryRun, JobStatus status) {
    return JobStatus.NOT_STARTED.equals(status);
  }
}
