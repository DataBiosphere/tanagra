package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZAttribute;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class ITEntitySearchByAttribute extends IndexTable {
  public static final String TABLE_NAME = "ESA";

  private final String entity;
  private final String attribute;
  private final ImmutableList<ColumnSchema> columnSchemas;

  public ITEntitySearchByAttribute(
      NameHelper namer, SZBigQuery.IndexData bigQueryConfig, SZEntity entity, String attribute) {
    super(namer, bigQueryConfig);
    this.entity = entity.name;

    List<ColumnSchema> columnSchemasBuilder = new ArrayList<>();
    // columns used for primary id or clustering cannot be repeated

    SZAttribute idAttribute =
        entity.attributes.stream()
            .filter(attr -> attr.name.equals(entity.idAttribute))
            .findFirst()
            .orElseThrow();
    columnSchemasBuilder.add(
        new ColumnSchema(
            idAttribute.name, ConfigReader.deserializeDataType(idAttribute.dataType), false, true));

    SZAttribute searchAttribute =
        entity.attributes.stream()
            .filter(attr -> attr.name.equals(attribute))
            .findFirst()
            .orElseThrow();
    columnSchemasBuilder.add(
        new ColumnSchema(
            searchAttribute.name,
            ConfigReader.deserializeDataType(searchAttribute.dataType),
            false,
            false));
    this.attribute = searchAttribute.name;
    this.columnSchemas = ImmutableList.copyOf(columnSchemasBuilder);
  }

  @Override
  public String getTableBaseName() {
    return TABLE_NAME + "_" + entity + "_" + attribute;
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return columnSchemas;
  }

  public String getEntity() {
    return entity;
  }

  public String getAttribute() {
    return attribute;
  }
}
