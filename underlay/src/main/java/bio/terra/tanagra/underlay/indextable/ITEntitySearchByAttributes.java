package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.underlay.ColumnSchema;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.serialization.SZAttributeSearch;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class ITEntitySearchByAttributes extends IndexTable {
  public static final String TABLE_NAME = "ESA";
  private final String entity;
  private final ImmutableList<String> attributeNames;
  private final boolean includeNullValues;
  private final boolean includeEntityMainColumns;
  private final ImmutableList<ColumnSchema> columnSchemas;

  public ITEntitySearchByAttributes(
      NameHelper namer,
      SZBigQuery.IndexData bigQueryConfig,
      ITEntityMain entityMain,
      SZEntity entity,
      SZAttributeSearch attributeSearch) {
    super(namer, bigQueryConfig);
    this.entity = entity.name;

    this.attributeNames =
        ImmutableList.copyOf(
            attributeSearch.attributes.stream()
                .map(attribute -> entity.getAttribute(attribute).name)
                .toList());

    List<ColumnSchema> attrSchemas = new ArrayList<>();
    entityMain
        .getColumnSchemas()
        .forEach(
            colSchema -> {
              String colName = colSchema.getColumnName();

              if (entity.idAttribute.equals(colName) || (attributeNames.contains(colName))) {
                // id + attr in tables optimized for search (clustered) cannot be repeated
                attrSchemas.add(
                    new ColumnSchema(
                        colName, colSchema.getDataType(), false, colSchema.isRequired()));
              } else if (attributeSearch.includeEntityMainColumns) {
                attrSchemas.add(
                    new ColumnSchema(
                        colName,
                        colSchema.getDataType(),
                        colSchema.isDataTypeRepeated(),
                        colSchema.isRequired()));
              }
            });
    this.columnSchemas = ImmutableList.copyOf(attrSchemas);
    this.includeNullValues = attributeSearch.includeNullValues;
    this.includeEntityMainColumns = attributeSearch.includeEntityMainColumns;
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

  public boolean includeEntityMainColumns() {
    return includeEntityMainColumns;
  }
}
