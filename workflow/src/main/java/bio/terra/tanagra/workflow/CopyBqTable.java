package bio.terra.tanagra.workflow;

import bio.terra.tanagra.proto.underlay.Column;
import bio.terra.tanagra.proto.underlay.ColumnId;
import bio.terra.tanagra.proto.underlay.Dataset;
import bio.terra.tanagra.proto.underlay.Dataset.BigQueryDataset;
import bio.terra.tanagra.proto.underlay.EntityMapping;
import bio.terra.tanagra.proto.underlay.Table;
import bio.terra.tanagra.proto.underlay.Underlay;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.TypedRead.Method;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// DO NOT SUBMIT comment me
public class CopyBqTable {
  private CopyBqTable() {}

  private static final Logger LOG = LoggerFactory.getLogger(CopyBqTable.class);

  /** Options supported by {@link CopyBqTable}. */
  public interface CopyBqTableOptions extends BigQueryOptions, CloudSqlOptions, UnderlayOptions {}

  public static void main(String[] args) throws IOException, SQLException {
    CopyBqTableOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(CopyBqTable.CopyBqTableOptions.class);

    Underlay underlay = UnderlayOptions.readLocalUnderlay(options);
    // DO NOT SUBMIT copy all tables/datasets?
    Dataset dataset = underlay.getDatasetsList().get(0);

    SerializableFunction<Void, DataSource> dataSourceProvider =
        JdbcIO.PoolableDataSourceProvider.of(CloudSqlOptions.createDataSourceConfiguration(options));

    Pipeline pipeline = Pipeline.create(options);
    DataSource localDataSource = dataSourceProvider.apply(null);
    for (Table table : dataset.getTablesList()) {
      createSqlTable(underlay, dataset, table, localDataSource);
      pipeline.apply("begin " + table.getName(), new CopyTable(dataset.getBigQueryDataset(), table, dataSourceProvider));
    }
    // TODO create additional indexes on new table.

    pipeline.run().waitUntilFinish();
  }

  private static void createSqlTable(Underlay underlay, Dataset dataset, Table table, DataSource dataSource)
      throws SQLException {
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

  /** Copies the contents of a BigQuery table to a SQL table.
   * <p>The SQL table must already exist. */
  private static class CopyTable extends PTransform<PBegin, PDone> {
    /** The BigQuery dataset to read the table from. */
    private final BigQueryDataset dataset;

    /** The schema of the table to be copied. */
    private final Table table;
    /** A provider for a datasource to use to connect to the SQL table. */
    private final SerializableFunction<Void, DataSource> dataSourceProvider;

    private CopyTable(BigQueryDataset dataset, Table table,
        SerializableFunction<Void, DataSource> dataSourceProvider) {
      this.dataset = dataset;
      this.table = table;
      this.dataSourceProvider = dataSourceProvider;
    }

    @Override
    public PDone expand(PBegin begin) {
      TableReference tableReference =
          new TableReference()
              .setProjectId(dataset.getProjectId())
              .setDatasetId(dataset.getDatasetId())
              .setTableId(table.getName());
       PCollection<TableRow> rows = begin.apply(
           "bqRead " + table.getName(),
          BigQueryIO.readTableRows()
              .from(tableReference)
              .withMethod(Method.DIRECT_READ)
              .withSelectedFields(
                  table.getColumnsList().stream()
                      .map(Column::getName)
                      .collect(Collectors.toList())));
       return rows
          .apply(
              "jdbc write " + table.getName(),
              JdbcIO.<TableRow>write()
                  .withDataSourceProviderFn(dataSourceProvider)
                  .withStatement(insertSql())
                  .withPreparedStatementSetter(
                      new JdbcIO.PreparedStatementSetter<TableRow>() {
                        @Override
                        public void setParameters(TableRow tableRow, PreparedStatement query)
                            throws SQLException {
                          // Set the value for each column in the tableRow on the prepared statement.
                          for (int i = 0; i < table.getColumnsList().size(); ++i) {
                            Column column = table.getColumns(i);
                            Optional<String> val =
                                Optional.ofNullable(tableRow.get(column.getName()))
                                    .map(Object::toString);
                            int parameterIndex = i + 1; // PreparedStatement parameters are 1-indexed.
                            switch (column.getDataType()) {
                              case FLOAT:
                                if (val.isPresent()) {
                                  query.setFloat(parameterIndex, Float.parseFloat(val.get()));
                                } else {
                                  query.setNull(parameterIndex, Types.FLOAT);
                                }
                                break;
                              case INT64:
                                if (val.isPresent()) {
                                  query.setLong(parameterIndex, Long.parseLong(val.get()));
                                } else {
                                  query.setNull(parameterIndex, Types.BIGINT);
                                }
                                break;
                              case STRING:
                                query.setString(parameterIndex, val.orElse(null));
                                break;
                              default:
                                throw new UnsupportedOperationException(
                                    String.format(
                                        "Unknown Column DataType %s", column.getDataType()));
                            }
                          }
                        }
                      }));
    }

    private String insertSql() {

      ImmutableList<String> columnNames =
          table.getColumnsList().stream()
              .map(Column::getName)
              .collect(ImmutableList.toImmutableList());
      // Do a postgres upsert so that this is tolerates Beam retries.
      // INSERT INTO table (column0, ..., columnN) VALUES (?, ..., ?) ON CONFLICT DO NOTHING;
     return
          String.format(
              "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT DO NOTHING;",
              table.getName(),
              String.join(", ", columnNames),
              String.join(", ", Collections.nCopies(columnNames.size(), "?")));
    }
  }

  /** Creates an SQL string for creating a SQL table based on ta {@link Table}. */
  private static String makeCreateTableSql(Underlay underlay, Dataset dataset, Table table) {
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
            + "CREATE TABLE ${tableName} (${columnsClause}${primaryKey});";
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
    String createStatement = StringSubstitutor.replace(createTemplate, params);
    return createStatement;
  }

  /** Returns the name of the column that should be the primary key for the table based on the entity mappings in the underlay. */
  private static Optional<String> getPrimaryKeyColumn(Underlay underlay, Dataset dataset, Table table) {
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
    Optional<String> primaryKeyColumn = primaryKeyColumns.stream().findFirst();
    return primaryKeyColumn;
  }
}
