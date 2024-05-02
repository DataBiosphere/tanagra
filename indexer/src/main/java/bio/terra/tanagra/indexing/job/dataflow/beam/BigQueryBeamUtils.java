package bio.terra.tanagra.indexing.job.dataflow.beam;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.underlay.ColumnSchema;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.apache.commons.text.StringSubstitutor;

public final class BigQueryBeamUtils {

  private BigQueryBeamUtils() {}

  /** Read the set of nodes from BQ and build a {@link PCollection} of just the node identifiers. */
  public static PCollection<Long> readNodesFromBQ(
      Pipeline pipeline, String sqlQuery, String idColumnName, String description) {
    PCollection<TableRow> allNodesBqRows =
        pipeline.apply(
            "read (id) rows: " + description,
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return allNodesBqRows.apply(
        "build (id) pcollection: " + description,
        MapElements.into(TypeDescriptors.longs())
            .via(tableRow -> Long.parseLong((String) tableRow.get(idColumnName))));
  }

  /**
   * Read all the two-field rows from BQ and build a {@link PCollection} of {@link KV} pairs
   * (field1, field2).
   */
  public static PCollection<KV<Long, Long>> readTwoFieldRowsFromBQ(
      Pipeline pipeline, String sqlQuery, String field1Name, String field2Name) {
    PCollection<TableRow> bqRows =
        pipeline.apply(
            "read all (" + field1Name + ", " + field2Name + ") rows",
            BigQueryIO.readTableRows()
                .fromQuery(sqlQuery)
                .withMethod(BigQueryIO.TypedRead.Method.EXPORT)
                .usingStandardSql());
    return bqRows.apply(
        "build (" + field1Name + ", " + field2Name + ") pcollection",
        MapElements.into(TypeDescriptors.kvs(TypeDescriptors.longs(), TypeDescriptors.longs()))
            .via(
                tableRow -> {
                  Long field1 = Long.parseLong((String) tableRow.get(field1Name));
                  Long field2 = Long.parseLong((String) tableRow.get(field2Name));
                  return KV.of(field1, field2);
                }));
  }

  public static String getTableSqlPath(String projectId, String datasetId, String tableName) {
    final String template = "${projectId}:${datasetId}.${tableName}";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("projectId", projectId)
            .put("datasetId", datasetId)
            .put("tableName", tableName)
            .build();
    return StringSubstitutor.replace(template, params);
  }

  public static TableSchema getBigQueryTableSchema(List<ColumnSchema> columns) {
    List<TableFieldSchema> fieldSchemas =
        sortedStream(columns)
            .map(
                c -> {
                  LegacySQLTypeName columnDataType = fromDataType(c.getDataType());
                  return new TableFieldSchema()
                      .setName(c.getColumnName())
                      .setType(columnDataType.name())
                      .setMode(c.isRequired() ? "REQUIRED" : "NULLABLE");
                })
            .collect(Collectors.toList());
    return new TableSchema().setFields(fieldSchemas);
  }

  public static LegacySQLTypeName fromDataType(DataType sqlDataType) {
    switch (sqlDataType) {
      case STRING:
        return LegacySQLTypeName.STRING;
      case INT64:
        return LegacySQLTypeName.INTEGER;
      case BOOLEAN:
        return LegacySQLTypeName.BOOLEAN;
      case DATE:
        return LegacySQLTypeName.DATE;
      case DOUBLE:
        return LegacySQLTypeName.FLOAT;
      case TIMESTAMP:
        return LegacySQLTypeName.TIMESTAMP;
      default:
        throw new SystemException("Data type not supported for BigQuery: " + sqlDataType);
    }
  }

  private static Stream<ColumnSchema> sortedStream(List<ColumnSchema> columns) {
    return columns.stream().sorted(Comparator.comparing(ColumnSchema::getColumnName));
  }
}
