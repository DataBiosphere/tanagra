package bio.terra.tanagra.workflow;

import static org.junit.Assert.assertEquals;

import bio.terra.tanagra.proto.underlay.Column;
import bio.terra.tanagra.proto.underlay.DataType;
import bio.terra.tanagra.proto.underlay.Dataset.BigQueryDataset;
import bio.terra.tanagra.proto.underlay.Table;
import javax.sql.DataSource;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.junit.Test;

public class CopyBigQueryTableTest {

  @Test
  public void insertSql() {
    CopyBigQueryTable copyTable =
        CopyBigQueryTable.builder()
            .table(
                Table.newBuilder()
                    .setName("foo")
                    .addColumns(Column.newBuilder().setName("bar").setDataType(DataType.INT64))
                    .addColumns(Column.newBuilder().setName("baz").setDataType(DataType.FLOAT))
                    .build())
            .dataset(BigQueryDataset.getDefaultInstance())
            .dataSourceProvider((SerializableFunction<Void, DataSource>) input -> null)
            .build();

    assertEquals(
        "INSERT INTO foo (bar, baz) VALUES (?, ?) ON CONFLICT DO NOTHING;", copyTable.insertSql());
  }
}
