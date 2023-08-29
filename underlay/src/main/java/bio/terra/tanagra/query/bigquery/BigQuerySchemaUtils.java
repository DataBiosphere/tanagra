package bio.terra.tanagra.query.bigquery;

import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.underlay.datapointer.BigQueryDataset;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.LegacySQLTypeName;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BigQuerySchemaUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(BigQuerySchemaUtils.class);

  private BigQuerySchemaUtils() {}

  public static List<Field> getBigQueryFieldList(List<ColumnSchema> columns) {
    return sortedStream(columns)
        .map(
            c -> {
              LOGGER.info("bigquery field: {}, {}", c.getColumnName(), c.getSqlDataType());
              LegacySQLTypeName columnDataType =
                  BigQueryDataset.fromSqlDataType(c.getSqlDataType());
              return Field.newBuilder(c.getColumnName(), columnDataType)
                  .setMode(c.isRequired() ? Field.Mode.REQUIRED : Field.Mode.NULLABLE)
                  .build();
            })
        .collect(Collectors.toList());
  }

  public static TableSchema getBigQueryTableSchema(List<ColumnSchema> columns) {
    List<TableFieldSchema> fieldSchemas =
        sortedStream(columns)
            .map(
                c -> {
                  LOGGER.info(
                      "bigquery field schema: {}, {}", c.getColumnName(), c.getSqlDataType());
                  LegacySQLTypeName columnDataType =
                      BigQueryDataset.fromSqlDataType(c.getSqlDataType());
                  return new TableFieldSchema()
                      .setName(c.getColumnName())
                      .setType(columnDataType.name())
                      .setMode(c.isRequired() ? "REQUIRED" : "NULLABLE");
                })
            .collect(Collectors.toList());
    return new TableSchema().setFields(fieldSchemas);
  }

  private static Stream<ColumnSchema> sortedStream(List<ColumnSchema> columns) {
    return columns.stream().sorted(Comparator.comparing(c -> c.getColumnName()));
  }
}
