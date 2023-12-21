package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.api.shared.DataType;
import bio.terra.tanagra.query2.sql.SqlColumnSchema;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.underlay.ConfigReader;
import bio.terra.tanagra.underlay.NameHelper;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.serialization.SZBigQuery;
import bio.terra.tanagra.underlay.serialization.SZEntity;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public final class ITEntityMain extends IndexTable {
  public static final String TABLE_NAME = "ENT";
  public static final String NO_HIERARCHY_NAME = "NOHIER";
  private final String entity;
  private final ImmutableList<SqlColumnSchema> columnSchemas;

  public ITEntityMain(
      NameHelper namer,
      SZBigQuery.IndexData bigQueryConfig,
      String entity,
      List<SZEntity.Attribute> szAttributes,
      Set<SZEntity.Hierarchy> szHierarchies,
      boolean hasTextSearch,
      Set<String> entityGroupsWithCounts) {
    super(namer, bigQueryConfig);
    this.entity = entity;

    List<SqlColumnSchema> columnSchemasBuilder = new ArrayList<>();
    // Build the attribute columns.
    szAttributes.stream()
        .forEach(
            szAttribute -> {
              columnSchemasBuilder.add(
                  new SqlColumnSchema(
                      szAttribute.name, ConfigReader.deserializeDataType(szAttribute.dataType)));
              if (szAttribute.displayFieldName != null) {
                columnSchemasBuilder.add(
                    new SqlColumnSchema(
                        getAttributeDisplayFieldName(szAttribute.name),
                        ColumnTemplate.ATTRIBUTE_DISPLAY.getDataType()));
              }
            });

    // Build the hierarchy columns.
    szHierarchies.stream()
        .forEach(
            szHierarchy -> {
              columnSchemasBuilder.add(
                  new SqlColumnSchema(
                      getHierarchyPathFieldName(szHierarchy.name),
                      ColumnTemplate.HIERARCHY_PATH.getDataType()));
              columnSchemasBuilder.add(
                  new SqlColumnSchema(
                      getHierarchyNumChildrenFieldName(szHierarchy.name),
                      ColumnTemplate.HIERARCHY_NUMCHILDREN.getDataType()));
            });

    // Build the text search column.
    if (hasTextSearch) {
      columnSchemasBuilder.add(
          new SqlColumnSchema(
              namer.getReservedFieldName(ColumnTemplate.TEXT_SEARCH.getColumnPrefix()),
              ColumnTemplate.TEXT_SEARCH.getDataType()));
    }

    // Build the relationship columns.
    entityGroupsWithCounts.stream()
        .forEach(
            entityGroup -> {
              // Build the no hierarchy column.
              columnSchemasBuilder.add(
                  new SqlColumnSchema(
                      getEntityGroupCountFieldName(entityGroup, null),
                      ColumnTemplate.RELATIONSHIP_COUNT.getDataType()));

              // Build the hierarchy columns.
              szHierarchies.stream()
                  .forEach(
                      szHierarchy ->
                          columnSchemasBuilder.add(
                              new SqlColumnSchema(
                                  getEntityGroupCountFieldName(entityGroup, szHierarchy.name),
                                  ColumnTemplate.RELATIONSHIP_COUNT.getDataType())));
            });
    this.columnSchemas = ImmutableList.copyOf(columnSchemasBuilder);
  }

  public String getEntity() {
    return entity;
  }

  @Override
  public String getTableBaseName() {
    return TABLE_NAME + "_" + entity;
  }

  @Override
  public ImmutableList<SqlColumnSchema> getColumnSchemas() {
    return columnSchemas;
  }

  public SqlField getAttributeValueField(String attribute) {
    return new SqlField.Builder().tablePointer(getTablePointer()).columnName(attribute).build();
  }

  public SqlColumnSchema getAttributeValueColumnSchema(Attribute attribute) {
    return new SqlColumnSchema(attribute.getName(), attribute.getDataType());
  }

  public SqlField getAttributeDisplayField(String attribute) {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(getAttributeDisplayFieldName(attribute))
        .build();
  }

  private String getAttributeDisplayFieldName(String attribute) {
    return namer.getReservedFieldName(
        ColumnTemplate.ATTRIBUTE_DISPLAY.getColumnNamePrefixed(attribute));
  }

  public SqlField getHierarchyPathField(String hierarchy) {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(getHierarchyPathFieldName(hierarchy))
        .build();
  }

  public SqlColumnSchema getHierarchyPathColumnSchema(String hierarchy) {
    return new SqlColumnSchema(
        getHierarchyPathFieldName(hierarchy), ColumnTemplate.HIERARCHY_PATH.getDataType());
  }

  private String getHierarchyPathFieldName(String hierarchy) {
    return namer.getReservedFieldName(
        ColumnTemplate.HIERARCHY_PATH.getColumnNamePrefixed(hierarchy));
  }

  public SqlField getHierarchyNumChildrenField(String hierarchy) {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(getHierarchyNumChildrenFieldName(hierarchy))
        .build();
  }

  public SqlColumnSchema getHierarchyNumChildrenColumnSchema(String hierarchy) {
    return new SqlColumnSchema(
        getHierarchyNumChildrenFieldName(hierarchy),
        ColumnTemplate.HIERARCHY_NUMCHILDREN.getDataType());
  }

  private String getHierarchyNumChildrenFieldName(String hierarchy) {
    return namer.getReservedFieldName(
        ColumnTemplate.HIERARCHY_NUMCHILDREN.getColumnNamePrefixed(hierarchy));
  }

  public SqlField getEntityGroupCountField(String entityGroup, @Nullable String hierarchy) {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(getEntityGroupCountFieldName(entityGroup, hierarchy))
        .build();
  }

  public SqlColumnSchema getEntityGroupCountColumnSchema(
      String entityGroup, @Nullable String hierarchy) {
    return new SqlColumnSchema(
        getEntityGroupCountFieldName(entityGroup, hierarchy),
        ColumnTemplate.RELATIONSHIP_COUNT.getDataType());
  }

  private String getEntityGroupCountFieldName(String entityGroup, @Nullable String hierarchy) {
    return namer.getReservedFieldName(
        ColumnTemplate.RELATIONSHIP_COUNT.getColumnNamePrefixed(
            entityGroup + "_" + (hierarchy == null ? NO_HIERARCHY_NAME : hierarchy)));
  }

  public SqlField getTextSearchField() {
    return new SqlField.Builder()
        .tablePointer(getTablePointer())
        .columnName(getTextSearchFieldName())
        .build();
  }

  private String getTextSearchFieldName() {
    return namer.getReservedFieldName(ColumnTemplate.TEXT_SEARCH.getColumnPrefix());
  }

  public SqlColumnSchema getTextSearchColumnSchema() {
    return new SqlColumnSchema(
        namer.getReservedFieldName(ColumnTemplate.TEXT_SEARCH.getColumnPrefix()),
        ColumnTemplate.TEXT_SEARCH.getDataType());
  }

  public enum ColumnTemplate {
    ATTRIBUTE_DISPLAY("DISP", DataType.STRING),
    HIERARCHY_PATH("PATH", DataType.STRING),
    HIERARCHY_NUMCHILDREN("NUMCH", DataType.INT64),
    TEXT_SEARCH("TXT", DataType.STRING),
    RELATIONSHIP_COUNT("RCNT", DataType.INT64);

    private final String columnPrefix;
    private final DataType dataType;

    ColumnTemplate(String columnPrefix, DataType dataType) {
      this.columnPrefix = columnPrefix;
      this.dataType = dataType;
    }

    public String getColumnPrefix() {
      return columnPrefix;
    }

    public DataType getDataType() {
      return dataType;
    }

    public String getColumnNamePrefixed(String baseColumnName) {
      return columnPrefix + "_" + baseColumnName;
    }
  }
}
