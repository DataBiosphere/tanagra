package bio.terra.tanagra.service.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.proto.underlay.ArrayColumnFilter;
import bio.terra.tanagra.proto.underlay.ArrayColumnFilterOperator;
import bio.terra.tanagra.proto.underlay.BinaryColumnFilter;
import bio.terra.tanagra.proto.underlay.BinaryColumnFilterOperator;
import bio.terra.tanagra.proto.underlay.ColumnId;
import bio.terra.tanagra.proto.underlay.EntityMapping;
import bio.terra.tanagra.proto.underlay.TableFilter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ArrayColumnFilter} used to define an {@link EntityMapping} with a {@link
 * TableFilter}. Some of these tests check for validation in the conversion from underlay proto ->
 * Java class. Other tests check the SQL string generated for queries against this underlay.
 */
public class ArrayColumnFilterTest extends ColumnFilterTest {
  @Test
  @DisplayName(
      "underlay conversion fails when one of the sub-filters' table doesn't match the entity pk table")
  void subFilterColumnAndPrimaryKeyFromDifferentTables() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setArrayColumnFilter(
                ArrayColumnFilter.newBuilder()
                    .addBinaryColumnFilters(
                        BinaryColumnFilter.newBuilder()
                            .setColumn(
                                ColumnId.newBuilder()
                                    .setDataset("datasetA")
                                    .setTable("tableB")
                                    .setColumn("columnC")
                                    .build())
                            .setOperator(BinaryColumnFilterOperator.LESS_THAN)
                            .setInt64Val(45)
                            .build())
                    .setOperator(ArrayColumnFilterOperator.AND)
                    .build())
            .build();

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> convertUnderlayFromProto(tableFilterProto),
            "exception thrown when one of the sub-filters' table doesn't match entity primary key table");
    assertThat(
        "exception message says that binary column filter table doesn't match entity primary key table",
        exception.getMessage(),
        Matchers.containsString(
            "Binary table filter column is not in the same table as the entity primary key"));
  }

  @Test
  @DisplayName("underlay conversion fails when there are no sub-filters")
  void noSubFilters() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setArrayColumnFilter(
                ArrayColumnFilter.newBuilder().setOperator(ArrayColumnFilterOperator.OR).build())
            .build();

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> convertUnderlayFromProto(tableFilterProto),
            "exception thrown when array column filter contains no sub-filters");
    assertThat(
        "exception message says that array column filter doesn't contain any sub-filters",
        exception.getMessage(),
        Matchers.containsString("Array column filter contains no sub-filters"));
  }

  @Test
  @DisplayName("correct SQL string when array column filter contains a single sub-filter")
  void singleSubFilter() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setArrayColumnFilter(
                ArrayColumnFilter.newBuilder()
                    .addBinaryColumnFilters(
                        BinaryColumnFilter.newBuilder()
                            .setColumn(
                                ColumnId.newBuilder()
                                    .setDataset("datasetA")
                                    .setTable("tableA")
                                    .setColumn("columnA")
                                    .build())
                            .setOperator(BinaryColumnFilterOperator.EQUALS)
                            .setStringVal("stringValueA")
                            .build())
                    .setOperator(ArrayColumnFilterOperator.AND)
                    .build())
            .build();

    bio.terra.tanagra.service.underlay.Underlay underlay =
        convertUnderlayFromProto(tableFilterProto);
    String generatedSql = generateSql(underlay, getEntityDatasetForEntityA(underlay));
    assertEquals(
        "SELECT entitya_alias.columnA AS attributeA "
            + "FROM (SELECT * FROM `my-project-id.my-dataset-id`.tableA WHERE columnA = 'stringValueA') AS entitya_alias "
            + "WHERE TRUE",
        generatedSql);
  }

  @Test
  @DisplayName("correct SQL string when array column filter contains multiple binary sub-filters")
  void multipleBinarySubFilters() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setArrayColumnFilter(
                ArrayColumnFilter.newBuilder()
                    .addBinaryColumnFilters(
                        BinaryColumnFilter.newBuilder()
                            .setColumn(
                                ColumnId.newBuilder()
                                    .setDataset("datasetA")
                                    .setTable("tableA")
                                    .setColumn("columnA")
                                    .build())
                            .setOperator(BinaryColumnFilterOperator.EQUALS)
                            .setStringVal("stringValueA")
                            .build())
                    .addBinaryColumnFilters(
                        BinaryColumnFilter.newBuilder()
                            .setColumn(
                                ColumnId.newBuilder()
                                    .setDataset("datasetA")
                                    .setTable("tableA")
                                    .setColumn("columnB")
                                    .build())
                            .setOperator(BinaryColumnFilterOperator.LESS_THAN)
                            .setInt64Val(64)
                            .build())
                    .setOperator(ArrayColumnFilterOperator.OR)
                    .build())
            .build();

    bio.terra.tanagra.service.underlay.Underlay underlay =
        convertUnderlayFromProto(tableFilterProto);
    String generatedSql = generateSql(underlay, getEntityDatasetForEntityA(underlay));
    assertEquals(
        "SELECT entitya_alias.columnA AS attributeA "
            + "FROM (SELECT * FROM `my-project-id.my-dataset-id`.tableA WHERE columnA = 'stringValueA' OR columnB < 64) AS entitya_alias "
            + "WHERE TRUE",
        generatedSql);
  }

  @Test
  @DisplayName(
      "correct SQL string when array column filter contains multiple array and binary sub-filters")
  void multipleBinaryAndArraySubFilters() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setArrayColumnFilter(
                ArrayColumnFilter.newBuilder()
                    .addBinaryColumnFilters(
                        BinaryColumnFilter.newBuilder()
                            .setColumn(
                                ColumnId.newBuilder()
                                    .setDataset("datasetA")
                                    .setTable("tableA")
                                    .setColumn("columnA")
                                    .build())
                            .setOperator(BinaryColumnFilterOperator.EQUALS)
                            .setStringVal("stringValueA")
                            .build())
                    .addBinaryColumnFilters(
                        BinaryColumnFilter.newBuilder()
                            .setColumn(
                                ColumnId.newBuilder()
                                    .setDataset("datasetA")
                                    .setTable("tableA")
                                    .setColumn("columnB")
                                    .build())
                            .setOperator(BinaryColumnFilterOperator.LESS_THAN)
                            .setInt64Val(64)
                            .build())
                    .addArrayColumnFilters(
                        ArrayColumnFilter.newBuilder()
                            .addBinaryColumnFilters(
                                BinaryColumnFilter.newBuilder()
                                    .setColumn(
                                        ColumnId.newBuilder()
                                            .setDataset("datasetA")
                                            .setTable("tableA")
                                            .setColumn("columnA")
                                            .build())
                                    .setOperator(BinaryColumnFilterOperator.EQUALS)
                                    .setStringVal("stringValueB")
                                    .build())
                            .addBinaryColumnFilters(
                                BinaryColumnFilter.newBuilder()
                                    .setColumn(
                                        ColumnId.newBuilder()
                                            .setDataset("datasetA")
                                            .setTable("tableA")
                                            .setColumn("columnB")
                                            .build())
                                    .setOperator(BinaryColumnFilterOperator.LESS_THAN)
                                    .setInt64Val(104)
                                    .build())
                            .setOperator(ArrayColumnFilterOperator.AND)
                            .build())
                    .addArrayColumnFilters(
                        ArrayColumnFilter.newBuilder()
                            .addBinaryColumnFilters(
                                BinaryColumnFilter.newBuilder()
                                    .setColumn(
                                        ColumnId.newBuilder()
                                            .setDataset("datasetA")
                                            .setTable("tableA")
                                            .setColumn("columnA")
                                            .build())
                                    .setOperator(BinaryColumnFilterOperator.EQUALS)
                                    .setStringVal("stringValueC")
                                    .build())
                            .addBinaryColumnFilters(
                                BinaryColumnFilter.newBuilder()
                                    .setColumn(
                                        ColumnId.newBuilder()
                                            .setDataset("datasetA")
                                            .setTable("tableA")
                                            .setColumn("columnB")
                                            .build())
                                    .setOperator(BinaryColumnFilterOperator.GREATER_THAN)
                                    .setInt64Val(256)
                                    .build())
                            .setOperator(ArrayColumnFilterOperator.AND)
                            .build())
                    .setOperator(ArrayColumnFilterOperator.OR)
                    .build())
            .build();

    bio.terra.tanagra.service.underlay.Underlay underlay =
        convertUnderlayFromProto(tableFilterProto);
    String generatedSql = generateSql(underlay, getEntityDatasetForEntityA(underlay));
    assertEquals(
        "SELECT entitya_alias.columnA AS attributeA "
            + "FROM (SELECT * FROM `my-project-id.my-dataset-id`.tableA WHERE columnA = 'stringValueA' OR columnB < 64 OR (columnA = 'stringValueB' AND columnB < 104) OR (columnA = 'stringValueC' AND columnB > 256)) AS entitya_alias "
            + "WHERE TRUE",
        generatedSql);
  }
}
