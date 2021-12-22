package bio.terra.tanagra.service.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.tanagra.proto.underlay.Attribute;
import bio.terra.tanagra.proto.underlay.AttributeId;
import bio.terra.tanagra.proto.underlay.AttributeMapping;
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
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.Query;
import bio.terra.tanagra.service.search.SearchContext;
import bio.terra.tanagra.service.search.SqlVisitor;
import bio.terra.tanagra.service.search.Variable;
import bio.terra.tanagra.service.underlay.UnderlayConversion;
import bio.terra.tanagra.testing.BaseSpringUnitTest;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for {@link BinaryColumnFilter} used to define an {@link EntityMapping} with a {@link
 * TableFilter}. Some of these tests check for validation in the conversion from underlay proto ->
 * Java class. Other tests check the SQL string generated for queries against this underlay.
 */
public class BinaryColumnFilterTest extends BaseSpringUnitTest {
  @Autowired private QueryService queryService;

  // underlay proto without any entity mappings defined, so that we can vary the table filter in the
  // entity mapping in each test method
  private final Underlay.Builder underlayProtoBuilder =
      Underlay.newBuilder()
          .setName("underlayA")
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
                          .addColumns(
                              Column.newBuilder().setName("columnB").setDataType(DataType.INT64))
                          .build())
                  .addTables(
                      Table.newBuilder()
                          .setName("tableB")
                          .addColumns(
                              Column.newBuilder().setName("columnC").setDataType(DataType.INT64))
                          .build())
                  .build())
          .addEntities(
              Entity.newBuilder()
                  .setName("entityA")
                  .addAttributes(
                      Attribute.newBuilder()
                          .setName("attributeA")
                          .setDataType(DataType.STRING)
                          .build())
                  .addAttributes(
                      Attribute.newBuilder()
                          .setName("attributeB")
                          .setDataType(DataType.INT64)
                          .build())
                  .build())
          .addAttributeMappings(
              AttributeMapping.newBuilder()
                  .setAttribute(
                      AttributeId.newBuilder()
                          .setEntity("entityA")
                          .setAttribute("attributeA")
                          .build())
                  .setSimpleColumn(
                      AttributeMapping.SimpleColumn.newBuilder()
                          .setColumnId(
                              ColumnId.newBuilder()
                                  .setDataset("datasetA")
                                  .setTable("tableA")
                                  .setColumn("columnA")
                                  .build())
                          .build())
                  .build())
          .addAttributeMappings(
              AttributeMapping.newBuilder()
                  .setAttribute(
                      AttributeId.newBuilder()
                          .setEntity("entityA")
                          .setAttribute("attributeB")
                          .build())
                  .setSimpleColumn(
                      AttributeMapping.SimpleColumn.newBuilder()
                          .setColumnId(
                              ColumnId.newBuilder()
                                  .setDataset("datasetA")
                                  .setTable("tableA")
                                  .setColumn("columnB")
                                  .build())
                          .build())
                  .build());

  // entity mapping proto without any table filter defined, so that we can vary the table filter in
  // each test method
  private final EntityMapping.Builder entityMappingProtoBuilder =
      EntityMapping.newBuilder()
          .setEntity("entityA")
          .setPrimaryKey(
              ColumnId.newBuilder()
                  .setDataset("datasetA")
                  .setTable("tableA")
                  .setColumn("columnA")
                  .build());

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
    System.out.println(exception.getMessage());
    assertTrue(
        exception
            .getMessage()
            .contains(
                "Binary table filter column is not in the same table as the entity primary key"),
        "exception message says that binary column filter table doesn't match entity primary key table");
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
    System.out.println(exception.getMessage());
    assertTrue(
        exception
            .getMessage()
            .contains("Binary table filter value is a different data type than the column"),
        "exception message says that binary column filter value doesn't match the column data type");
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

  private bio.terra.tanagra.service.underlay.Underlay convertUnderlayFromProto(
      TableFilter tableFilterProto) {
    Underlay underlayProto =
        underlayProtoBuilder
            .addEntityMappings(entityMappingProtoBuilder.setTableFilter(tableFilterProto).build())
            .build();
    return UnderlayConversion.convert(underlayProto);
  }

  private EntityDataset getEntityDatasetForEntityA(
      bio.terra.tanagra.service.underlay.Underlay underlay) {
    bio.terra.tanagra.service.search.Entity entity =
        bio.terra.tanagra.service.search.Entity.builder()
            .underlay("underlayA")
            .name("entityA")
            .build();
    bio.terra.tanagra.service.search.Attribute attributeA =
        bio.terra.tanagra.service.search.Attribute.builder()
            .name("attributeA")
            .entity(entity)
            .dataType(bio.terra.tanagra.service.search.DataType.STRING)
            .build();

    return EntityDataset.builder()
        .primaryEntity(
            EntityVariable.create(
                underlay.entities().get("entityA"), Variable.create("entitya_alias")))
        .selectedAttributes(ImmutableList.of(attributeA))
        .filter(Filter.NullFilter.INSTANCE)
        .build();
  }

  private String generateSql(
      bio.terra.tanagra.service.underlay.Underlay underlay, EntityDataset entityDataset) {
    Query query = queryService.createQuery(entityDataset);
    return new SqlVisitor(SearchContext.builder().underlay(underlay).build()).createSql(query);
  }
}
