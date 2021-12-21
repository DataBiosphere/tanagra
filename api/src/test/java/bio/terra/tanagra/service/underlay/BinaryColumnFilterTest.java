package bio.terra.tanagra.service.underlay;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.proto.underlay.BinaryColumnFilter;
import bio.terra.tanagra.proto.underlay.BinaryColumnFilterOperator;
import bio.terra.tanagra.proto.underlay.Column;
import bio.terra.tanagra.proto.underlay.ColumnId;
import bio.terra.tanagra.proto.underlay.DataType;
import bio.terra.tanagra.proto.underlay.Dataset;
import bio.terra.tanagra.proto.underlay.Entity;
import bio.terra.tanagra.proto.underlay.EntityMapping;
import bio.terra.tanagra.proto.underlay.Table;
import bio.terra.tanagra.proto.underlay.TableFilter;
import bio.terra.tanagra.proto.underlay.Underlay;
import org.junit.jupiter.api.Test;

public class BinaryColumnFilterTest {
  @Test
  void filterColumnAndPrimaryKeyFromDifferentTables() {
    Underlay underlayProto =
        Underlay.newBuilder()
            .setName("underlayA")
            .addEntities(Entity.newBuilder().setName("entityA").build())
            .addEntityMappings(
                EntityMapping.newBuilder()
                    .setEntity("entityA")
                    .setPrimaryKey(
                        ColumnId.newBuilder()
                            .setDataset("datasetA")
                            .setTable("tableA")
                            .setColumn("columnA")
                            .build())
                    .setTableFilter(
                        TableFilter.newBuilder()
                            .setBinaryColumnFilter(
                                BinaryColumnFilter.newBuilder()
                                    .setColumn(
                                        ColumnId.newBuilder()
                                            .setDataset("datasetA")
                                            .setTable("tableB")
                                            .setColumn("columnB")
                                            .build())
                                    .setOperator(BinaryColumnFilterOperator.LESS_THAN)
                                    .setInt64Val(45)
                                    .build())
                            .build())
                    .build())
            .addDatasets(
                Dataset.newBuilder()
                    .setName("datasetA")
                    .setBigQueryDataset(
                        Dataset.BigQueryDataset.newBuilder()
                            .setDatasetId("my-dataset-id")
                            .setProjectId("my-project-id")
                            .build())
                    .addTables(
                        Table.newBuilder()
                            .setName("tableA")
                            .addColumns(
                                Column.newBuilder().setName("columnA").setDataType(DataType.STRING))
                            .build())
                    .addTables(
                        Table.newBuilder()
                            .setName("tableB")
                            .addColumns(
                                Column.newBuilder().setName("columnB").setDataType(DataType.INT64))
                            .build())
                    .build())
            .build();
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> UnderlayConversion.convert(underlayProto),
            "exception thrown when binary column filter table doesn't match entity primary key table");
    assertTrue(
        exception
            .getMessage()
            .contains(
                "Binary table filter column is not in the same table as the entity primary key"),
        "exception message says that binary column filter table doesn't match entity primary key table");
  }
}
