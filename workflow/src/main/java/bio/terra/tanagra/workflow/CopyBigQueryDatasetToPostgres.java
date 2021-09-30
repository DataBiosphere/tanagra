package bio.terra.tanagra.workflow;

import bio.terra.tanagra.proto.underlay.ColumnId;
import bio.terra.tanagra.proto.underlay.Dataset;
import bio.terra.tanagra.proto.underlay.EntityMapping;
import bio.terra.tanagra.proto.underlay.Table;
import bio.terra.tanagra.proto.underlay.Underlay;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A batch Apache Beam pipeline for copying data from BigQuery to CloudSQL Postgres instances. */
public class CopyBigQueryDatasetToPostgres {
  private CopyBigQueryDatasetToPostgres() {}

  private static final Logger LOG = LoggerFactory.getLogger(CopyBigQueryDatasetToPostgres.class);

  /** Options supported by {@link CopyBigQueryDatasetToPostgres}. */
  public interface CopyBigQueryDatasetOptions
      extends BigQueryOptions, CloudSqlOptions, UnderlayOptions {}

  public static void main(String[] args) throws IOException, SQLException {
    CopyBigQueryDatasetOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CopyBigQueryDatasetOptions.class);

    Underlay underlay = UnderlayOptions.readLocalUnderlay(options);
    // TODO consider how to configure the underlay to mark which datasets should be copied.
    Dataset dataset =
        underlay.getDatasetsList().stream().filter(Dataset::hasBigQueryDataset).findFirst().get();

    SerializableFunction<Void, DataSource> dataSourceProvider =
        JdbcIO.PoolableDataSourceProvider.of(
            CloudSqlOptions.createDataSourceConfiguration(options));

    Pipeline pipeline = Pipeline.create(options);
    DataSource localDataSource = dataSourceProvider.apply(null);
    for (Table table : dataset.getTablesList()) {
      createSqlTable(underlay, dataset, table, localDataSource);
      CopyBigQueryTableToPostgres copyTable =
          CopyBigQueryTableToPostgres.builder()
              .dataset(dataset.getBigQueryDataset())
              .table(table)
              .dataSourceProvider(dataSourceProvider)
              .build();
      pipeline.apply("begin " + copyTable.transformNameSuffix(), copyTable);
    }

    pipeline.run().waitUntilFinish();
  }

  private static void createSqlTable(
      Underlay underlay, Dataset dataset, Table table, DataSource dataSource) throws SQLException {
    String createStatement = makeCreateTableSql(underlay, dataset, table);
    LOG.info("Creating table: " + createStatement);
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(createStatement);
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }
  }

  /** Creates an SQL string for creating a SQL table based on {@link Table}. */
  @VisibleForTesting
  static String makeCreateTableSql(Underlay underlay, Dataset dataset, Table table) {
    Optional<String> primaryKeyColumn = getPrimaryKeyColumn(underlay, dataset, table);

    // (columnA typeA, columnB typeB...)
    String columnsClause =
        table.getColumnsList().stream()
            .map(
                column -> {
                  // https://www.postgresql.org/docs/13/datatype.html
                  String postgresType;
                  switch (column.getDataType()) {
                    case FLOAT:
                      postgresType = "real";
                      break;
                    case INT64:
                      postgresType = "bigint";
                      break;
                    case STRING:
                      postgresType = "text";
                      break;
                    default:
                      throw new UnsupportedOperationException(
                          String.format(
                              "Unable to find postgres type for DataType '%s' in column '%s'",
                              column.getDataType(), column.getName()));
                  }
                  return String.format("%s %s", column.getName(), postgresType);
                })
            .collect(Collectors.joining(",\n"));

    String createTemplate =
        "DROP TABLE IF EXISTS ${tableName};\n"
            + "CREATE TABLE ${tableName} (\n${columnsClause}${primaryKey});";
    Map<String, String> params =
        ImmutableMap.<String, String>builder()
            .put("tableName", table.getName())
            .put("columnsClause", columnsClause)
            .put(
                "primaryKey",
                primaryKeyColumn
                    .map(column -> String.format(", PRIMARY KEY(%s)", column))
                    .orElse(""))
            .build();
    return StringSubstitutor.replace(createTemplate, params);
  }

  /**
   * Returns the name of the column that should be the primary key for the table based on the entity
   * mappings in the underlay.
   */
  private static Optional<String> getPrimaryKeyColumn(
      Underlay underlay, Dataset dataset, Table table) {
    List<String> primaryKeyColumns =
        underlay.getEntityMappingsList().stream()
            .map(EntityMapping::getPrimaryKey)
            .filter(
                primaryKey ->
                    primaryKey.getDataset().equals(dataset.getName())
                        && primaryKey.getTable().equals(table.getName()))
            .map(ColumnId::getColumn)
            .distinct()
            .collect(Collectors.toList());
    if (primaryKeyColumns.size() > 1) {
      throw new IllegalArgumentException(
          String.format(
              "Unable to pick a primary key for table '%s' with too many entity primary keys: %s",
              table.getName(), primaryKeyColumns));
    }
    return primaryKeyColumns.stream().findFirst();
  }
}
