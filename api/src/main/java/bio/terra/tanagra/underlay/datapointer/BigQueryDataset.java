package bio.terra.tanagra.underlay.datapointer;

import bio.terra.tanagra.serialization.datapointer.UFBigQueryDataset;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.FieldPointer;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public class BigQueryDataset extends DataPointer {
  private final String projectId;
  private final String datasetId;

  public BigQueryDataset(String name, String projectId, String datasetId) {
    super(name);
    this.projectId = projectId;
    this.datasetId = datasetId;
  }

  public static BigQueryDataset fromSerialized(UFBigQueryDataset serialized) {
    if (serialized.getProjectId() == null || serialized.getProjectId().isEmpty()) {
      throw new IllegalArgumentException("No BigQuery project ID defined");
    }
    if (serialized.getDatasetId() == null || serialized.getDatasetId().isEmpty()) {
      throw new IllegalArgumentException("No BigQuery dataset ID defined");
    }
    return new BigQueryDataset(
        serialized.getName(), serialized.getProjectId(), serialized.getDatasetId());
  }

  @Override
  public Type getType() {
    return Type.BQ_DATASET;
  }

  @Override
  public String getTableSQL(String tableName) {
    String template = "`${projectId}.${datasetId}`.${tableName}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("projectId", projectId)
            .put("datasetId", datasetId)
            .put("tableName", tableName)
            .build();
    return StringSubstitutor.replace(template, params);
  }

  @Override
  public String getTablePathForIndexing(String tableName) {
    String template = "${projectId}:${datasetId}.${tableName}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("projectId", projectId)
            .put("datasetId", datasetId)
            .put("tableName", tableName)
            .build();
    return StringSubstitutor.replace(template, params);
  }

  @Override
  public Literal.DataType lookupDatatype(FieldPointer fieldPointer) {
    // if this is a foreign-key field pointer, then we want the datatype of the foreign table field,
    // not the key field
    String tableName =
        fieldPointer.isForeignKey()
            ? fieldPointer.getForeignTablePointer().getTableName()
            : fieldPointer.getTablePointer().getTableName();
    String columnName =
        fieldPointer.isForeignKey()
            ? fieldPointer.getForeignColumnName()
            : fieldPointer.getColumnName();

    Schema tableSchema =
        GoogleBigQuery.getDefault().getTableSchemaWithCaching(projectId, datasetId, tableName);
    Field field = tableSchema.getFields().get(columnName);
    if (LegacySQLTypeName.STRING.equals(field.getType())) {
      return Literal.DataType.STRING;
    } else if (LegacySQLTypeName.INTEGER.equals(field.getType())) {
      return Literal.DataType.INT64;
    } else if (LegacySQLTypeName.BOOLEAN.equals(field.getType())) {
      return Literal.DataType.BOOLEAN;
    } else {
      throw new UnsupportedOperationException(
          "BigQuery SQL data type not supported: " + field.getType());
    }
  }

  public String getProjectId() {
    return projectId;
  }

  public String getDatasetId() {
    return datasetId;
  }
}
