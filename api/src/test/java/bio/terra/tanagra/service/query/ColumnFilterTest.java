package bio.terra.tanagra.service.query;

import bio.terra.tanagra.proto.underlay.Attribute;
import bio.terra.tanagra.proto.underlay.AttributeId;
import bio.terra.tanagra.proto.underlay.AttributeMapping;
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
import org.springframework.beans.factory.annotation.Autowired;

/** Base class for different table filters in an underlay definition. */
public class ColumnFilterTest extends BaseSpringUnitTest {
  @Autowired private QueryService queryService;

  // underlay proto without any entity mappings defined, so that we can vary the table filter in the
  // entity mapping in each test method
  protected final Underlay.Builder underlayProtoBuilder =
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
  protected final EntityMapping.Builder entityMappingProtoBuilder =
      EntityMapping.newBuilder()
          .setEntity("entityA")
          .setPrimaryKey(
              ColumnId.newBuilder()
                  .setDataset("datasetA")
                  .setTable("tableA")
                  .setColumn("columnA")
                  .build());

  protected bio.terra.tanagra.service.underlay.Underlay convertUnderlayFromProto(
      TableFilter tableFilterProto) {
    Underlay underlayProto =
        underlayProtoBuilder
            .addEntityMappings(entityMappingProtoBuilder.setTableFilter(tableFilterProto).build())
            .build();
    return UnderlayConversion.convert(underlayProto);
  }

  protected EntityDataset getEntityDatasetForEntityA(
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

  protected String generateSql(
      bio.terra.tanagra.service.underlay.Underlay underlay, EntityDataset entityDataset) {
    Query query = queryService.createQuery(entityDataset);
    return new SqlVisitor(
            SearchContext.builder()
                .underlay(underlay)
                .randomNumberGenerator(randomNumberGenerator)
                .build())
        .createSql(query);
  }
}
