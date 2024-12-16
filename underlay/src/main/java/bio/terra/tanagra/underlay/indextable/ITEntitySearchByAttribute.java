package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZAttribute;
import bio.terra.tanagra.underlay.serialization.SZAttributeSearch;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class ITEntitySearchByAttribute extends IndexTable {
  public static final String TABLE_NAME = "ESA";

  private final String entity;
  private final ImmutableList<String> attributeNames;
  private final boolean includeNullValues;
  private final ImmutableList<ColumnSchema> columnSchemas;

  public ITEntitySearchByAttribute(
      NameHelper namer,
      SZBigQuery.IndexData bigQueryConfig,
      SZEntity entity,
      SZAttributeSearch attributeSearch) {
    super(namer, bigQueryConfig);
    this.entity = entity.name;

    // id + columns in tables optimized for search (clustered) cannot be repeated
    List<String> attrNames = new ArrayList<>();
    List<ColumnSchema> attrSchemas = new ArrayList<>();

    SZAttribute idAttribute = entity.getAttribute(entity.idAttribute);
    attrSchemas.add(
        new ColumnSchema(
            idAttribute.name, ConfigReader.deserializeDataType(idAttribute.dataType), false, true));

    attributeSearch.attributes.forEach(
        attribute -> {
          SZAttribute searchAttribute = entity.getAttribute(attribute);
          attrNames.add(searchAttribute.name);
          attrSchemas.add(
              new ColumnSchema(
                  searchAttribute.name,
                  ConfigReader.deserializeDataType(searchAttribute.dataType),
                  false,
                  false));
        });

    this.attributeNames = ImmutableList.copyOf(attrNames);
    this.includeNullValues = attributeSearch.includeNullValues;
    this.columnSchemas = ImmutableList.copyOf(attrSchemas);
  }

  @Override
  public String getTableBaseName() {
    return TABLE_NAME + "_" + entity + "_" + String.join("_", attributeNames);
  }

  @Override
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return columnSchemas;
  }

  public String getEntity() {
    return entity;
  }

  public ImmutableList<String> getAttributeNames() {
    return attributeNames;
  }

  public boolean includeNullValues() {
    return includeNullValues;
  }
}
