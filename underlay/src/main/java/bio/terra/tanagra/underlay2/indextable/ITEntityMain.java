package bio.terra.tanagra.underlay2.indextable;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.ColumnSchema;
import bio.terra.tanagra.query.DataPointer;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay2.NameHelper;
import bio.terra.tanagra.underlay2.serialization.SZEntity;
import com.google.common.collect.ImmutableList;
import java.util.*;
import javax.annotation.Nullable;

public final class ITEntityMain extends IndexTable {
  public static final String TABLE_NAME = "ENT";
  private static final String NO_HIERARCHY_NAME = "NOHIER";
  private final String entity;
  private final ImmutableList<ColumnSchema> columnSchemas;

  public ITEntityMain(
      NameHelper namer,
      DataPointer dataPointer,
      String entity,
      Set<SZEntity.Attribute> szAttributes,
      Set<SZEntity.Hierarchy> szHierarchies,
      boolean hasTextSearch,
      Set<String> entityGroupsWithCounts) {
    super(namer, dataPointer);
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
              if (szAttribute.valueFieldName != null) {
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
              szAttributes.stream()
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

  private String getHierarchyNumChildrenFieldName(String hierarchy) {
    return namer.getReservedFieldName(
        ColumnTemplate.HIERARCHY_PATH.getColumnNamePrefixed(hierarchy));
  }

  public FieldPointer getEntityGroupCountField(String entityGroup, @Nullable String hierarchy) {
    return new FieldPointer.Builder()
        .tablePointer(getTablePointer())
        .columnName(getEntityGroupCountFieldName(entityGroup, hierarchy))
        .build();
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
