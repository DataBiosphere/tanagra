package bio.terra.tanagra.underlay.indextable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.FieldPointer;
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
  private final ImmutableList<ColumnSchema> columnSchemas;

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

    List<ColumnSchema> columnSchemasBuilder = new ArrayList<>();
    // Build the attribute columns.
    szAttributes.stream()
        .forEach(
            szAttribute -> {
              columnSchemasBuilder.add(
                  new ColumnSchema(
                      szAttribute.name,
                      CellValue.SQLDataType.fromUnderlayDataType(szAttribute.dataType)));
              if (szAttribute.displayFieldName != null) {
                columnSchemasBuilder.add(
                    new ColumnSchema(
                        getAttributeDisplayFieldName(szAttribute.name),
                        ColumnTemplate.ATTRIBUTE_DISPLAY.getSqlDataType()));
              }
            });

    // Build the hierarchy columns.
    szHierarchies.stream()
        .forEach(
            szHierarchy -> {
              columnSchemasBuilder.add(
                  new ColumnSchema(
                      getHierarchyPathFieldName(szHierarchy.name),
                      ColumnTemplate.HIERARCHY_PATH.getSqlDataType()));
              columnSchemasBuilder.add(
                  new ColumnSchema(
                      getHierarchyNumChildrenFieldName(szHierarchy.name),
                      ColumnTemplate.HIERARCHY_NUMCHILDREN.getSqlDataType()));
            });

    // Build the text search column.
    if (hasTextSearch) {
      columnSchemasBuilder.add(
          new ColumnSchema(
              namer.getReservedFieldName(ColumnTemplate.TEXT_SEARCH.getColumnPrefix()),
              ColumnTemplate.TEXT_SEARCH.getSqlDataType()));
    }

    // Build the relationship columns.
    entityGroupsWithCounts.stream()
        .forEach(
            entityGroup -> {
              // Build the no hierarchy column.
              columnSchemasBuilder.add(
                  new ColumnSchema(
                      getEntityGroupCountFieldName(entityGroup, null),
                      ColumnTemplate.RELATIONSHIP_COUNT.getSqlDataType()));

              // Build the hierarchy columns.
              szHierarchies.stream()
                  .forEach(
                      szHierarchy ->
                          columnSchemasBuilder.add(
                              new ColumnSchema(
                                  getEntityGroupCountFieldName(entityGroup, szHierarchy.name),
                                  ColumnTemplate.RELATIONSHIP_COUNT.getSqlDataType())));
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
  public ImmutableList<ColumnSchema> getColumnSchemas() {
    return columnSchemas;
  }

  public FieldPointer getAttributeValueField(String attribute) {
    return new FieldPointer.Builder().tablePointer(getTablePointer()).columnName(attribute).build();
  }

  public ColumnSchema getAttributeValueColumnSchema(Attribute attribute) {
    return new ColumnSchema(
        attribute.getName(), CellValue.SQLDataType.fromUnderlayDataType(attribute.getDataType()));
  }

  public FieldPointer getAttributeDisplayField(String attribute) {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(getAttributeDisplayFieldName(attribute))
        .build();
  }

  private String getAttributeDisplayFieldName(String attribute) {
    return namer.getReservedFieldName(
        ColumnTemplate.ATTRIBUTE_DISPLAY.getColumnNamePrefixed(attribute));
  }

  public FieldPointer getHierarchyPathField(String hierarchy) {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(getHierarchyPathFieldName(hierarchy))
        .build();
  }

  public ColumnSchema getHierarchyPathColumnSchema(String hierarchy) {
    return new ColumnSchema(
        getHierarchyPathFieldName(hierarchy), ColumnTemplate.HIERARCHY_PATH.getSqlDataType());
  }

  private String getHierarchyPathFieldName(String hierarchy) {
    return namer.getReservedFieldName(
        ColumnTemplate.HIERARCHY_PATH.getColumnNamePrefixed(hierarchy));
  }

  public FieldPointer getHierarchyNumChildrenField(String hierarchy) {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(getHierarchyNumChildrenFieldName(hierarchy))
        .build();
  }

  public ColumnSchema getHierarchyNumChildrenColumnSchema(String hierarchy) {
    return new ColumnSchema(
        getHierarchyNumChildrenFieldName(hierarchy),
        ColumnTemplate.HIERARCHY_NUMCHILDREN.getSqlDataType());
  }

  private String getHierarchyNumChildrenFieldName(String hierarchy) {
    return namer.getReservedFieldName(
        ColumnTemplate.HIERARCHY_NUMCHILDREN.getColumnNamePrefixed(hierarchy));
  }

  public FieldPointer getEntityGroupCountField(String entityGroup, @Nullable String hierarchy) {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(getEntityGroupCountFieldName(entityGroup, hierarchy))
        .build();
  }

  public ColumnSchema getEntityGroupCountColumnSchema(
      String entityGroup, @Nullable String hierarchy) {
    return new ColumnSchema(
        getEntityGroupCountFieldName(entityGroup, hierarchy),
        ColumnTemplate.RELATIONSHIP_COUNT.getSqlDataType());
  }

  private String getEntityGroupCountFieldName(String entityGroup, @Nullable String hierarchy) {
    return namer.getReservedFieldName(
        ColumnTemplate.RELATIONSHIP_COUNT.getColumnNamePrefixed(
            entityGroup + "_" + (hierarchy == null ? NO_HIERARCHY_NAME : hierarchy)));
  }

  public FieldPointer getTextSearchField() {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(getTextSearchFieldName())
        .build();
  }

  private String getTextSearchFieldName() {
    return namer.getReservedFieldName(ColumnTemplate.TEXT_SEARCH.getColumnPrefix());
  }

  public ColumnSchema getTextSearchColumnSchema() {
    return new ColumnSchema(
        namer.getReservedFieldName(ColumnTemplate.TEXT_SEARCH.getColumnPrefix()),
        ColumnTemplate.TEXT_SEARCH.getSqlDataType());
  }

  public enum ColumnTemplate {
    ATTRIBUTE_DISPLAY("DISP", CellValue.SQLDataType.STRING),
    HIERARCHY_PATH("PATH", CellValue.SQLDataType.STRING),
    HIERARCHY_NUMCHILDREN("NUMCH", CellValue.SQLDataType.INT64),
    TEXT_SEARCH("TXT", CellValue.SQLDataType.STRING),
    RELATIONSHIP_COUNT("RCNT", CellValue.SQLDataType.INT64);

    private final String columnPrefix;
    private final CellValue.SQLDataType sqlDataType;

    ColumnTemplate(String columnPrefix, CellValue.SQLDataType sqlDataType) {
      this.columnPrefix = columnPrefix;
      this.sqlDataType = sqlDataType;
    }

    public String getColumnPrefix() {
      return columnPrefix;
    }

    public CellValue.SQLDataType getSqlDataType() {
      return sqlDataType;
    }

    public String getColumnNamePrefixed(String baseColumnName) {
      return columnPrefix + "_" + baseColumnName;
    }
  }
}
