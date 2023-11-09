package bio.terra.tanagra.underlay.datapointer;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.QueryExecutor;
import bio.terra.tanagra.query.TablePointer;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.query.bigquery.BigQueryExecutor;
import bio.terra.tanagra.serialization.datapointer.UFBigQueryDataset;
import bio.terra.tanagra.underlay.DataPointer;
import bio.terra.tanagra.utils.GoogleBigQuery;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;

public final class BigQueryDataset extends DataPointer {
  private static final String DEFAULT_REGION = "us-central1";
  private static final String DEFAULT_WORKER_MACHINE_TYPE = "n1-standard-4"; // "n1-highmem-8"
  private final String projectId;
  private final String datasetId;
  private final String queryProjectId;
  private final String dataflowServiceAccountEmail;
  private final String dataflowTempLocation;
  private final String dataflowRegion;
  private final String dataflowWorkerMachineType;
  private final boolean dataflowUsePublicIps;
  private final String dataflowSubnetworkName;

  private GoogleBigQuery bigQueryService;
  private BigQueryExecutor queryExecutor;

  @SuppressWarnings("checkstyle:ParameterNumber")
  private BigQueryDataset(
      String name,
      String projectId,
      String datasetId,
      String queryProjectId,
      String dataflowServiceAccountEmail,
      String dataflowTempLocation,
      String dataflowRegion,
      String dataflowWorkerMachineType,
      boolean dataflowUsePublicIps,
      String dataflowSubnetworkName) {
    super(name);
    this.projectId = projectId;
    this.datasetId = datasetId;
    this.queryProjectId = queryProjectId;
    this.dataflowServiceAccountEmail = dataflowServiceAccountEmail;
    this.dataflowTempLocation = dataflowTempLocation;
    this.dataflowRegion = dataflowRegion;
    this.dataflowWorkerMachineType = dataflowWorkerMachineType;
    this.dataflowUsePublicIps = dataflowUsePublicIps;
    this.dataflowSubnetworkName = dataflowSubnetworkName;
  }

  public static BigQueryDataset fromSerialized(UFBigQueryDataset serialized) {
    if (serialized.getProjectId() == null || serialized.getProjectId().isEmpty()) {
      throw new InvalidConfigException("No BigQuery project ID defined");
    }
    if (serialized.getDatasetId() == null || serialized.getDatasetId().isEmpty()) {
      throw new InvalidConfigException("No BigQuery dataset ID defined");
    }
    // Default query project id is the same project where the data lives.
    // This property lets users specify a different project to run the queries in.
    // This is useful for e.g. public datasets, where you don't have permission to run queries
    // there.
    String queryProjectId = serialized.getQueryProjectId();
    if (queryProjectId == null || queryProjectId.isEmpty()) {
      queryProjectId = serialized.getProjectId();
    }

    return new BigQueryDataset(
        serialized.getName(),
        serialized.getProjectId(),
        serialized.getDatasetId(),
        queryProjectId,
        serialized.getDataflowServiceAccountEmail(),
        serialized.getDataflowTempLocation(),
        serialized.getDataflowRegion(),
        serialized.getDataflowWorkerMachineType(),
        serialized.isDataflowUsePublicIps(),
        serialized.getDataflowSubnetworkName());
  }

  @Override
  public Type getType() {
    return Type.BQ_DATASET;
  }

  @Override
  public QueryExecutor getQueryExecutor() {
    if (queryExecutor == null) {
      queryExecutor = new BigQueryExecutor(projectId, datasetId, queryProjectId);
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
    // If this is a foreign-key field pointer, then we want the data type of the foreign table
    // field, not the key field.
    TablePointer tablePointer =
        fieldPointer.isForeignKey()
            ? fieldPointer.getForeignTablePointer()
            : fieldPointer.getTablePointer();
    String columnName =
        fieldPointer.isForeignKey()
            ? fieldPointer.getForeignColumnName()
            : fieldPointer.getColumnName();

    Schema tableSchema;
    if (tablePointer.isRawSql() || fieldPointer.hasSqlFunctionWrapper()) {
      // If the table is a raw SQL string, then we can't fetch a table schema directly.
      // Instead, fetch a single row result and inspect the data types of that.
      TableVariable tableVar = TableVariable.forPrimary(tablePointer);
      List<TableVariable> tableVars = List.of(tableVar);
      FieldVariable fieldVarStar = fieldPointer.buildVariable(tableVar, tableVars, columnName);
      Query queryOneRow =
          new Query.Builder().select(List.of(fieldVarStar)).tables(tableVars).limit(1).build();
      tableSchema = getBigQueryService().getQuerySchemaWithCaching(queryOneRow.renderSQL());
    } else {
      // If the table is not a raw SQL string, then just fetch the table schema directly.
      tableSchema =
          getBigQueryService()
              .getTableSchemaWithCaching(projectId, datasetId, tablePointer.getTableName());
    }

    LegacySQLTypeName fieldType = tableSchema.getFields().get(columnName).getType();
    if (LegacySQLTypeName.STRING.equals(fieldType)) {
      return Literal.DataType.STRING;
    } else if (LegacySQLTypeName.INTEGER.equals(fieldType)) {
      return Literal.DataType.INT64;
    } else if (LegacySQLTypeName.BOOLEAN.equals(fieldType)) {
      return Literal.DataType.BOOLEAN;
    } else if (LegacySQLTypeName.DATE.equals(fieldType)) {
      return Literal.DataType.DATE;
    } else if (LegacySQLTypeName.FLOAT.equals(fieldType)
        || LegacySQLTypeName.NUMERIC.equals(fieldType)) {
      return Literal.DataType.DOUBLE;
    } else if (LegacySQLTypeName.TIMESTAMP.equals(fieldType)) {
      return Literal.DataType.TIMESTAMP;
    } else {
      throw new SystemException(
          "BigQuery SQL data type not supported: " + fieldType + ", " + columnName);
    }
  }

  public static LegacySQLTypeName fromSqlDataType(CellValue.SQLDataType sqlDataType) {
    switch (sqlDataType) {
      case STRING:
        return LegacySQLTypeName.STRING;
      case INT64:
        return LegacySQLTypeName.INTEGER;
      case BOOLEAN:
        return LegacySQLTypeName.BOOLEAN;
      case DATE:
        return LegacySQLTypeName.DATE;
      case FLOAT:
        return LegacySQLTypeName.FLOAT;
      case TIMESTAMP:
        return LegacySQLTypeName.TIMESTAMP;
      default:
        throw new SystemException("SQL data type not supported for BigQuery: " + sqlDataType);
    }
  }

  public GoogleBigQuery getBigQueryService() {
    if (bigQueryService == null) {
      GoogleCredentials credentials;
      try {
        credentials = GoogleCredentials.getApplicationDefault();
      } catch (IOException ioEx) {
        throw new SystemException("Error loading application default credentials", ioEx);
      }
      bigQueryService = new GoogleBigQuery(credentials, queryProjectId);
    }
    return bigQueryService;
  }

  public String getProjectId() {
    return projectId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public String getQueryProjectId() {
    return queryProjectId;
  }

  public String getDataflowServiceAccountEmail() {
    return dataflowServiceAccountEmail;
  }

  public String getDataflowTempLocation() {
    return dataflowTempLocation;
  }

  public String getDataflowRegion() {
    return dataflowRegion == null ? DEFAULT_REGION : dataflowRegion;
  }

  public String getDataflowWorkerMachineType() {
    return dataflowWorkerMachineType == null
        ? DEFAULT_WORKER_MACHINE_TYPE
        : dataflowWorkerMachineType;
  }

  public boolean isDataflowUsePublicIps() {
    return dataflowUsePublicIps;
  }

  public String getDataflowSubnetworkName() {
    return dataflowSubnetworkName;
  }
}