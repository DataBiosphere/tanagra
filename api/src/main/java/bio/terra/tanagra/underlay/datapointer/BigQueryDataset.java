package bio.terra.tanagra.underlay.datapointer;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.query.bigquery.BigQueryExecutor;
import bio.terra.tanagra.serialization.datapointer.UFBigQueryDataset;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.underlay.FieldPointer;
import bio.terra.tanagra.underlay.Literal;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public final class BigQueryDataset extends DataPointer {
  private final String projectId;
  private final String datasetId;
  private GoogleBigQuery bigQueryService;
  private BigQueryExecutor queryExecutor;

  private BigQueryDataset(String name, String projectId, String datasetId) {
    super(name);
    this.projectId = projectId;
    this.datasetId = datasetId;
  }

  public static BigQueryDataset fromSerialized(UFBigQueryDataset serialized) {
    if (serialized.getProjectId() == null || serialized.getProjectId().isEmpty()) {
      throw new InvalidConfigException("No BigQuery project ID defined");
    }
    if (serialized.getDatasetId() == null || serialized.getDatasetId().isEmpty()) {
      throw new InvalidConfigException("No BigQuery dataset ID defined");
    }
    return new BigQueryDataset(
        serialized.getName(), serialized.getProjectId(), serialized.getDatasetId());
  }

  @Override
  public Type getType() {
    return Type.BQ_DATASET;
  }

  @Override
  public QueryExecutor getQueryExecutor() {
    if (queryExecutor == null) {
      queryExecutor = new BigQueryExecutor(getBigQueryService());
    }
    return queryExecutor;
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
        getBigQueryService().getTableSchemaWithCaching(projectId, datasetId, tableName);
    Field field = tableSchema.getFields().get(columnName);
    if (LegacySQLTypeName.STRING.equals(field.getType())
        || LegacySQLTypeName.DATE.equals(field.getType())) {
      return Literal.DataType.STRING;
    } else if (LegacySQLTypeName.INTEGER.equals(field.getType())) {
      return Literal.DataType.INT64;
    } else if (LegacySQLTypeName.BOOLEAN.equals(field.getType())) {
      return Literal.DataType.BOOLEAN;
    } else {
      throw new SystemException("BigQuery SQL data type not supported: " + field.getType());
    }
  }

  public GoogleBigQuery getBigQueryService() {
    if (bigQueryService == null) {
      String adcFile = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
      System.out.println("env var: " + adcFile);
      if (adcFile != null) {
        Path adc = Path.of(adcFile);
        System.out.println("file " + adc.toAbsolutePath() + " exists: " + adc.toFile().exists());
        if (!adc.toFile().exists()) {
          throw new RuntimeException("ADC file doesn't exist: " + adc.toAbsolutePath());
        }
      } else {
        throw new RuntimeException("ADC not found: env var is null");
      }
      GoogleCredentials credentials;
      try {
        credentials = GoogleCredentials.getApplicationDefault();
      } catch (IOException ioEx) {
        throw new SystemException("Error loading application default credentials", ioEx);
      }
      bigQueryService = new GoogleBigQuery(credentials, projectId);
    }
    return bigQueryService;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getDatasetId() {
    return datasetId;
  }
}
