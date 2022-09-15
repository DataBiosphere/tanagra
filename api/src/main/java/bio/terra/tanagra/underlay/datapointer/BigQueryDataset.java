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
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public final class BigQueryDataset extends DataPointer {
  private final String projectId;
  private final String datasetId;
  private final Path serviceAccountKeyFile;
  // TODO: revisit how to point Tanagra at the right credentials for indexing/querying
  private ServiceAccountCredentials serviceAccountCredentials;
  private GoogleBigQuery bigQueryService;
  private BigQueryExecutor queryExecutor;

  private BigQueryDataset(
      String name, String projectId, String datasetId, Path serviceAccountKeyFile) {
    super(name);
    this.projectId = projectId;
    this.datasetId = datasetId;
    this.serviceAccountKeyFile = serviceAccountKeyFile;
  }

  public static BigQueryDataset fromSerialized(UFBigQueryDataset serialized) {
    if (serialized.getProjectId() == null || serialized.getProjectId().isEmpty()) {
      throw new InvalidConfigException("No BigQuery project ID defined");
    }
    if (serialized.getDatasetId() == null || serialized.getDatasetId().isEmpty()) {
      throw new InvalidConfigException("No BigQuery dataset ID defined");
    }
    if (serialized.getServiceAccountKeyFile() == null
        || serialized.getServiceAccountKeyFile().isEmpty()) {
      throw new InvalidConfigException("No service account key file defined for BigQuery dataset");
    }
    Path serviceAccountKeyFile = Path.of(serialized.getServiceAccountKeyFile());
    if (!serviceAccountKeyFile.toFile().exists()) {
      throw new InvalidConfigException(
          "Service account key file does not point to a valid path: "
              + serviceAccountKeyFile.toAbsolutePath());
    }
    return new BigQueryDataset(
        serialized.getName(),
        serialized.getProjectId(),
        serialized.getDatasetId(),
        serviceAccountKeyFile);
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

  private GoogleBigQuery getBigQueryService() {
    if (bigQueryService == null) {
      try {
        serviceAccountCredentials =
            ServiceAccountCredentials.fromStream(Files.newInputStream(serviceAccountKeyFile));
      } catch (IOException ioEx) {
        throw new SystemException(
            "Error reading service account key file: " + serviceAccountKeyFile, ioEx);
      }
      bigQueryService = new GoogleBigQuery(serviceAccountCredentials, projectId);
    }
    return bigQueryService;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public Path getServiceAccountKeyFile() {
    return serviceAccountKeyFile;
  }
}
