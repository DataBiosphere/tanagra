package bio.terra.tanagra.service.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.tanagra.proto.underlay.BinaryColumnFilter;
import bio.terra.tanagra.proto.underlay.BinaryColumnFilterOperator;
import bio.terra.tanagra.proto.underlay.ColumnId;
import bio.terra.tanagra.proto.underlay.EntityMapping;
import bio.terra.tanagra.proto.underlay.TableFilter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BinaryColumnFilter} used to define an {@link EntityMapping} with a {@link
 * TableFilter}. Some of these tests check for validation in the conversion from underlay proto ->
 * Java class. Other tests check the SQL string generated for queries against this underlay.
 */
public class BinaryColumnFilterTest extends ColumnFilterTest {
  @Test
  @DisplayName(
      "underlay conversion fails when binary column filter table doesn't match the entity pk table")
  void filterColumnAndPrimaryKeyFromDifferentTables() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setBinaryColumnFilter(
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
            .build();

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> convertUnderlayFromProto(tableFilterProto),
            "exception thrown when binary column filter table doesn't match entity primary key table");
    assertThat(
        "exception message says that binary column filter table doesn't match entity primary key table",
        exception.getMessage(),
        Matchers.containsString(
            "Binary table filter column is not in the same table as the entity primary key"));
  }

  @Test
  @DisplayName(
      "underlay conversion fails when binary column filter value doesn't match the column data type")
  void filterColumnAndValueHaveDifferentDataTypes() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setBinaryColumnFilter(
                BinaryColumnFilter.newBuilder()
                    .setColumn(
                        ColumnId.newBuilder()
                            .setDataset("datasetA")
                            .setTable("tableA")
                            .setColumn("columnA")
                            .build())
                    .setOperator(BinaryColumnFilterOperator.LESS_THAN)
                    .setInt64Val(64)
                    .build())
            .build();

    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> convertUnderlayFromProto(tableFilterProto),
            "exception thrown when binary column filter value doesn't match the column data type");
    assertThat(
        "exception message says that binary column filter value doesn't match the column data type",
        exception.getMessage(),
        Matchers.containsString(
            "Binary table filter value is a different data type than the column"));
  }

  @Test
  @DisplayName("correct SQL string when binary column filter uses = operator and string value")
  void generateSqlWithStringValueEqualsOperator() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setBinaryColumnFilter(
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
  @DisplayName("correct SQL string when binary column filter uses != operator and string value")
  void generateSqlWithStringValueNotEqualsOperator() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setBinaryColumnFilter(
                BinaryColumnFilter.newBuilder()
                    .setColumn(
                        ColumnId.newBuilder()
                            .setDataset("datasetA")
                            .setTable("tableA")
                            .setColumn("columnA")
                            .build())
                    .setOperator(BinaryColumnFilterOperator.NOT_EQUALS)
                    .setStringVal("stringValueA")
                    .build())
            .build();

    bio.terra.tanagra.service.underlay.Underlay underlay =
        convertUnderlayFromProto(tableFilterProto);
    String generatedSql = generateSql(underlay, getEntityDatasetForEntityA(underlay));
    assertEquals(
        "SELECT entitya_alias.columnA AS attributeA "
            + "FROM (SELECT * FROM `my-project-id.my-dataset-id`.tableA WHERE columnA != 'stringValueA') AS entitya_alias "
            + "WHERE TRUE",
        generatedSql);
  }

  @Test
  @DisplayName("correct SQL string when binary column filter uses < operator and int64 value")
  void generateSqlWithIntValueLTOperator() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setBinaryColumnFilter(
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
            .build();

    bio.terra.tanagra.service.underlay.Underlay underlay =
        convertUnderlayFromProto(tableFilterProto);
    String generatedSql = generateSql(underlay, getEntityDatasetForEntityA(underlay));
    assertEquals(
        "SELECT entitya_alias.columnA AS attributeA "
            + "FROM (SELECT * FROM `my-project-id.my-dataset-id`.tableA WHERE columnB < 64) AS entitya_alias "
            + "WHERE TRUE",
        generatedSql);
  }

  @Test
  @DisplayName("correct SQL string when binary column filter uses > operator and int64 value")
  void generateSqlWithIntValueGTOperator() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setBinaryColumnFilter(
                BinaryColumnFilter.newBuilder()
                    .setColumn(
                        ColumnId.newBuilder()
                            .setDataset("datasetA")
                            .setTable("tableA")
                            .setColumn("columnB")
                            .build())
                    .setOperator(BinaryColumnFilterOperator.GREATER_THAN)
                    .setInt64Val(92)
                    .build())
            .build();

    bio.terra.tanagra.service.underlay.Underlay underlay =
        convertUnderlayFromProto(tableFilterProto);
    String generatedSql = generateSql(underlay, getEntityDatasetForEntityA(underlay));
    assertEquals(
        "SELECT entitya_alias.columnA AS attributeA "
            + "FROM (SELECT * FROM `my-project-id.my-dataset-id`.tableA WHERE columnB > 92) AS entitya_alias "
            + "WHERE TRUE",
        generatedSql);
  }

  @Test
  @DisplayName("correct SQL string when binary column filter uses = operator and null value")
  void generateSqlWithIsNull() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setBinaryColumnFilter(
                BinaryColumnFilter.newBuilder()
                    .setColumn(
                        ColumnId.newBuilder()
                            .setDataset("datasetA")
                            .setTable("tableA")
                            .setColumn("columnB")
                            .build())
                    .setOperator(BinaryColumnFilterOperator.EQUALS)
                    // no value set means null value
                    .build())
            .build();

    bio.terra.tanagra.service.underlay.Underlay underlay =
        convertUnderlayFromProto(tableFilterProto);
    String generatedSql = generateSql(underlay, getEntityDatasetForEntityA(underlay));
    assertEquals(
        "SELECT entitya_alias.columnA AS attributeA "
            + "FROM (SELECT * FROM `my-project-id.my-dataset-id`.tableA WHERE columnB IS NULL) AS entitya_alias "
            + "WHERE TRUE",
        generatedSql);
  }

  @Test
  @DisplayName("correct SQL string when binary column filter uses != operator and null value")
  void generateSqlWithIsNotNull() {
    TableFilter tableFilterProto =
        TableFilter.newBuilder()
            .setBinaryColumnFilter(
                BinaryColumnFilter.newBuilder()
                    .setColumn(
                        ColumnId.newBuilder()
                            .setDataset("datasetA")
                            .setTable("tableA")
                            .setColumn("columnB")
                            .build())
                    .setOperator(BinaryColumnFilterOperator.NOT_EQUALS)
                    // no value set means null value
                    .build())
            .build();

    bio.terra.tanagra.service.underlay.Underlay underlay =
        convertUnderlayFromProto(tableFilterProto);
    String generatedSql = generateSql(underlay, getEntityDatasetForEntityA(underlay));
    assertEquals(
        "SELECT entitya_alias.columnA AS attributeA "
            + "FROM (SELECT * FROM `my-project-id.my-dataset-id`.tableA WHERE columnB IS NOT NULL) AS entitya_alias "
            + "WHERE TRUE",
        generatedSql);
  }
}
