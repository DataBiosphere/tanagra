package bio.terra.tanagra.workflow;

import static org.junit.Assert.assertEquals;

import bio.terra.tanagra.proto.underlay.Column;
import bio.terra.tanagra.proto.underlay.ColumnId;
import bio.terra.tanagra.proto.underlay.DataType;
import bio.terra.tanagra.proto.underlay.Dataset;
import bio.terra.tanagra.proto.underlay.EntityMapping;
import bio.terra.tanagra.proto.underlay.Table;
import bio.terra.tanagra.proto.underlay.Underlay;
import org.junit.Test;

// TODO add an integration test.
public class CopyBigQueryDatasetTest {

  @Test
  public void makeCreateTableSql() {
    assertEquals(
        "DROP TABLE IF EXISTS foo;\n"
            + "CREATE TABLE foo (\n"
            + "c_int64 bigint,\n"
            + "c_float real,\n"
            + "c_string text);",
        CopyBigQueryDataset.makeCreateTableSql(
            Underlay.getDefaultInstance(),
            Dataset.getDefaultInstance(),
            Table.newBuilder()
                .setName("foo")
                .addColumns(Column.newBuilder().setName("c_int64").setDataType(DataType.INT64))
                .addColumns(Column.newBuilder().setName("c_float").setDataType(DataType.FLOAT))
                .addColumns(Column.newBuilder().setName("c_string").setDataType(DataType.STRING))
                .build()));
  }

  @Test
  public void makeCreateTableSqlWithPrimaryKey() {
    assertEquals(
        "DROP TABLE IF EXISTS foo;\n"
            + "CREATE TABLE foo (\n"
            + "c_int64 bigint, PRIMARY KEY(c_int64));",
        CopyBigQueryDataset.makeCreateTableSql(
            Underlay.newBuilder()
                .addEntityMappings(
                    EntityMapping.newBuilder()
                        .setEntity("e")
                        .setPrimaryKey(
                            ColumnId.newBuilder()
                                .setDataset("dataset")
                                .setTable("foo")
                                .setColumn("c_int64")
                                .build())
                        .build())
                .build(),
            Dataset.newBuilder().setName("dataset").build(),
            Table.newBuilder()
                .setName("foo")
                .addColumns(Column.newBuilder().setName("c_int64").setDataType(DataType.INT64))
                .build()));
  }
}
