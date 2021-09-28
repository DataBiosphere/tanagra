package bio.terra.tanagra.workflow;

import bio.terra.tanagra.proto.underlay.Column;
import bio.terra.tanagra.proto.underlay.Dataset;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.TypedRead.Method;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.DataSourceConfiguration;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.commons.text.StringSubstitutor;

// DO NOT SUBMIT comment me
public class CopyBqTable {
  private CopyBqTable() {}

  /** Options supported by {@link CopyBqTable}. */
  public interface CopyBqTableOptions extends BigQueryOptions, UnderlayOptions {
    @Description(
        "The full name of the CloudSQL instance where tables are being copied to: {projectId}:{region}:{instanceId}")
    String getCloudSqlInstanceName();

    void setCloudSqlInstanceName(String instanceName);

    @Description("The name of the CloudSQL database where tables are being copied.")
    String getCloudSqlDatabaseName();

    void setCloudSqlDatabaseName(String databaseName);

    @Description("The username for connecting to the CloudSQL database.")
    String getCloudSqlUserName();

    void setCloudSqlUserName(String userName);
  }

  public static void main(String[] args) throws IOException, SQLException {
    CopyBqTableOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(CopyBqTable.CopyBqTableOptions.class);

    Underlay underlay = UnderlayOptions.readLocalUnderlay(options);
    // DO NOT SUBMIT copy all tables/datasets?
    Dataset dataset = underlay.getDatasetsList().get(0);
    Table table =
        dataset.getTablesList().stream()
            .filter(t -> "concept".equals(t.getName()))
            .findFirst()
            .get();

    TableReference tableReference =
        new TableReference()
            .setProjectId(dataset.getBigQueryDataset().getProjectId())
            .setDatasetId(dataset.getBigQueryDataset().getDatasetId())
            .setTableId(table.getName());

    // Don't encode sensitive information in options as they are written in job logs.
    String dbPassword = System.getenv("POSTGRES_PWD");
    if (dbPassword == null || dbPassword.isEmpty()) {
      // DO NOT SUBMIT variable the env var.
      throw new IllegalArgumentException(
          "database password must be specified with environment variable POSTGRES_PASS");
    }

    String connectionUrl =
        String.format(
            "jdbc:postgresql://google/%s"
                + "?cloudSqlInstance=%s"
                + "&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
                + "&user=%s"
                + "&password=%s",
            options.getCloudSqlDatabaseName(),
            options.getCloudSqlInstanceName(),
            options.getCloudSqlUserName(),
            dbPassword);
    SerializableFunction<Void, DataSource> dataSourceProvider =
        JdbcIO.PoolableDataSourceProvider.of(
            DataSourceConfiguration.create("org.postgresql.Driver", connectionUrl));

    Pipeline pipeline = Pipeline.create(options);

    Optional<String> primaryKeyColumn = Optional.empty();
    String columnsClause =
        table.getColumnsList().stream()
            .map(
                column -> {
                  String postgresType;
                  // https://www.postgresql.org/docs/13/datatype.html
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

    try (Connection connection = dataSourceProvider.apply(null).getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(createStatement);
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }

    ImmutableList<String> columnNames =
        table.getColumnsList().stream()
            .map(Column::getName)
            .collect(ImmutableList.toImmutableList());
    // Do a postgres upsert so that this is tolerates Beam retries.
    // INSERT INTO table (column0, ..., columnN) VALUES (?, ..., ?) ON CONFLICT DO NOTHING;
    String statement =
        String.format(
            "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT DO NOTHING;",
            table.getName(),
            String.join(", ", columnNames),
            String.join(", ", Collections.nCopies(columnNames.size(), "?")));

    System.out.println(statement);

    pipeline
        .apply(
            BigQueryIO.readTableRows()
                .from(tableReference)
                .withMethod(Method.DIRECT_READ)
                .withSelectedFields(
                    table.getColumnsList().stream()
                        .map(Column::getName)
                        .collect(Collectors.toList())))
        .apply(
            JdbcIO.<TableRow>write()
                .withDataSourceProviderFn(dataSourceProvider)
                .withStatement(statement)
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
    // TODO create additional indexes on new table.

    pipeline.run().waitUntilFinish();
  }
}
