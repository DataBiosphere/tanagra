package bio.terra.tanagra.workflow;

import bio.terra.tanagra.proto.underlay.Column;
import bio.terra.tanagra.proto.underlay.Dataset;
import bio.terra.tanagra.proto.underlay.Table;
import bio.terra.tanagra.proto.underlay.Underlay;
import bio.terra.tanagra.workflow.FlattenHierarchy.FlattenHierarchyOptions;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.TypedRead.Method;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryOptions;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.io.jdbc.JdbcIO.DataSourceConfiguration;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.SerializableFunction;

// DO NOT SUBMIT comment me
public class CopyBqTable {
  private CopyBqTable() {}

  /** Options supported by {@link CopyBqTable}. */
  public interface CopyBqTableOptions extends BigQueryOptions {
    @Description(
        "The name of the CloudSQL database where tables are being copied."
    )
    String cloudSqlDatabaseName();

    void setCloudSqlDatabaseName(String databaseName);

    @Description("The username for connecting to the CloudSQL database.")
    String cloudSqlUserName();

    void setCloudSqlUserName(String userName);

    @Description("The full name of the CloudSQL instance where tables are being copied to: {projectId}:{region}:{instanceId}")
    String cloudSqlInstanceName();

    void setCloudSqlInstanceName(String instanceName);
  }


  public static void main(String[] args) {
    CopyBqTableOptions options =
        PipelineOptionsFactory
            .fromArgs(args).withValidation().as(CopyBqTable.CopyBqTableOptions.class);

    Underlay underlay = Underlay.newBuilder().build(); // do not submit load me
    Dataset dataset = underlay.getDatasetsList().get(0);
    Table table = dataset.getTables(0);

    TableReference tableReference = new TableReference().setProjectId(dataset.getBigQueryDataset().getProjectId())
        .setDatasetId(dataset.getBigQueryDataset().getProjectId())
        .setTableId(table.getName());

    // Don't encode sensitive information in options as they are written in job logs.
    String dbPassword = System.getenv("POSTGRES_PWD");
    if (dbPassword.isEmpty()) {
      // do not submit variable the env var.
      throw new IllegalArgumentException("database password must be specified with environment variable POSTGRES_PASS");
    }

    // reWriteBatchedInserts=true DO NOT SUBMIT
    String connectionUrl=
        String.format("jdbc:postgresql://google/%s"
            + "?cloudSqlInstance=%s"
            + "&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
            + "&user=%s"
            + "&password=%s", options.cloudSqlDatabaseName(),
            options.cloudSqlInstanceName(), options.cloudSqlUserName(), dbPassword

            );
    SerializableFunction<Void, DataSource> dataSourceProvider = JdbcIO.PoolableDataSourceProvider.of(DataSourceConfiguration.create("org.postgresql.Driver",connectionUrl)
        // DO NOT SUBMIT is withUsername withPassword needed?
        .withUsername(options.cloudSqlUserName())
        .withPassword(dbPassword));

    Pipeline pipeline = Pipeline.create(options);

    ImmutableList<String> columnNames = table.getColumnsList().stream().map(Column::getName).collect(
        ImmutableList.toImmutableList());
    // Do a postgres upsert so that this is tolerates Beam retries.
    // INSERT INTO table (column0, ..., columnN) VALUES (?, ..., ?) ON CONFLICT DO UPDATE
    String statement = String.format("INSERT INTO %s (%s) VALUES( %s) ON CONFLICT DO UPDATE",
        table.getName(), String.join(", ", columnNames), String.join(", ", Collections.nCopies(columnNames.size(), "?")));

    // DO NOT SUBMIT create table in postgres

    pipeline.apply(
        BigQueryIO.readTableRows()
            .from(tableReference)
            .withMethod(Method.DIRECT_READ)
            .withSelectedFields(table.getColumnsList().stream().map(Column::getName).collect(
                Collectors.toList())))
    .apply(JdbcIO.<TableRow>write()
        .withDataSourceProviderFn(dataSourceProvider)
        .withStatement(statement)
        .withPreparedStatementSetter(new JdbcIO.PreparedStatementSetter<TableRow>() {
          public void setParameters(TableRow tableRow, PreparedStatement query)
              throws SQLException {
            for (int i = 0; i < table.getColumnsList().size(); ++i) {
              Column column = table.getColumns(i);
              String val = tableRow.get(column.getName()).toString();
              switch (column.getDataType()) {
                case FLOAT:
                  query.setFloat(i, Float.parseFloat(val));
                  break;
                case INT64:
                  query.setLong(i, Long.parseLong(val));
                  break;
                case STRING:
                  query.setString(i, val);
                  break;
                default:
                  throw new UnsupportedOperationException(
                      String.format("Unknown Column DataType %s", column.getDataType()));
              }
            }
          }
        })
    );
    // TODO create additional indexes on new table.
  }
}
