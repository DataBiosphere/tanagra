package bio.terra.tanagra.indexing.job.dataflow.beam;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.exception.SystemException;
import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.indextable.ITHierarchyAncestorDescendant;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BigQueryBeamUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryBeamUtils.class);

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

  /**
   * Build a query to select all descendant-ancestor pairs from the ancestor-descendant table, and
   * the pipeline step to read the results.
   */
  public static PCollection<KV<Long, Long>> readDescendantAncestorRelationshipsFromBQ(
      Pipeline pipeline, ITHierarchyAncestorDescendant ancestorDescendantTable) {
    String descendantAncestorSql =
        "SELECT * FROM " + ancestorDescendantTable.getTablePointer().render();
    LOGGER.info("descendant-ancestor query: {}", descendantAncestorSql);
    return BigQueryBeamUtils.readTwoFieldRowsFromBQ(
        pipeline,
        descendantAncestorSql,
        ITHierarchyAncestorDescendant.Column.DESCENDANT.getSchema().getColumnName(),
        ITHierarchyAncestorDescendant.Column.ANCESTOR.getSchema().getColumnName());
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
    return switch (sqlDataType) {
      case STRING -> LegacySQLTypeName.STRING;
      case INT64 -> LegacySQLTypeName.INTEGER;
      case BOOLEAN -> LegacySQLTypeName.BOOLEAN;
      case DATE -> LegacySQLTypeName.DATE;
      case DOUBLE -> LegacySQLTypeName.FLOAT;
      case TIMESTAMP -> LegacySQLTypeName.TIMESTAMP;
      default -> throw new SystemException("Data type not supported for BigQuery: " + sqlDataType);
    };
  }

  private static Stream<ColumnSchema> sortedStream(List<ColumnSchema> columns) {
    return columns.stream().sorted(Comparator.comparing(ColumnSchema::getColumnName));
  }
}
