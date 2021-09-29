package bio.terra.tanagra.workflow;

import bio.terra.tanagra.proto.underlay.Column;
import bio.terra.tanagra.proto.underlay.Dataset.BigQueryDataset;
import bio.terra.tanagra.proto.underlay.Table;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.TypedRead.Method;
import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

/**
 * Copies the contents of a BigQuery table to a SQL table.
 *
 * <p>The SQL table must already exist.
 */
@AutoValue
abstract class CopyBigQueryTable extends PTransform<PBegin, PDone> {

  /** The BigQuery dataset to read the table from. */
  abstract BigQueryDataset dataset();

  /** The schema of the table to be copied. */
  abstract Table table();

  /** A provider for a datasource to use to connect to the SQL table. */
  abstract SerializableFunction<Void, DataSource> dataSourceProvider();

  /**
   * Returns a name to use in transforms to uniquely identify this {@link CopyBigQueryTable}
   * instances operations.
   */
  String transformNameSuffix() {
    return dataset().getDatasetId() + "." + table().getName();
  }

  @Override
  public PDone expand(PBegin begin) {
    TableReference tableReference =
        new TableReference()
            .setProjectId(dataset().getProjectId())
            .setDatasetId(dataset().getDatasetId())
            .setTableId(table().getName());
    PCollection<TableRow> rows =
        begin.apply(
            "bqRead " + transformNameSuffix(),
            BigQueryIO.readTableRows()
                .from(tableReference)
                .withMethod(Method.DIRECT_READ)
                .withSelectedFields(
                    table().getColumnsList().stream()
                        .map(Column::getName)
                        .collect(Collectors.toList())));
    return rows.apply(
        "jdbc write " + transformNameSuffix(),
        JdbcIO.<TableRow>write()
            .withDataSourceProviderFn(dataSourceProvider())
            .withStatement(insertSql())
            .withPreparedStatementSetter(
                new JdbcIO.PreparedStatementSetter<TableRow>() {
                  @Override
                  public void setParameters(TableRow tableRow, PreparedStatement query)
                      throws SQLException {
                    // Set the value for each column in the tableRow on the prepared statement.
                    for (int i = 0; i < table().getColumnsList().size(); ++i) {
                      Column column = table().getColumns(i);
                      Optional<String> val =
                          Optional.ofNullable(tableRow.get(column.getName())).map(Object::toString);
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
                              String.format("Unknown Column DataType %s", column.getDataType()));
                      }
                    }
                  }
                }));
  }

  @VisibleForTesting
  String insertSql() {
    ImmutableList<String> columnNames =
        table().getColumnsList().stream()
            .map(Column::getName)
            .collect(ImmutableList.toImmutableList());
    // Do a postgres upsert so that this is tolerates Beam retries.
    // INSERT INTO table (column0, ..., columnN) VALUES (?, ..., ?) ON CONFLICT DO NOTHING;
    return String.format(
        "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT DO NOTHING;",
        table().getName(),
        String.join(", ", columnNames),
        String.join(", ", Collections.nCopies(columnNames.size(), "?")));
  }

  public static Builder builder() {
    return new AutoValue_CopyBigQueryTable.Builder();
  }

  /** Builder for {@link CopyBigQueryTable}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder dataset(BigQueryDataset dataset);

    public abstract Builder table(Table table);

    public abstract Builder dataSourceProvider(
        SerializableFunction<Void, DataSource> dataSourceProvider);

    public abstract CopyBigQueryTable build();
  }
}
