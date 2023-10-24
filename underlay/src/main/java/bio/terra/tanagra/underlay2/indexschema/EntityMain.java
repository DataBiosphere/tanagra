package bio.terra.tanagra.underlay2.indexschema;

import bio.terra.tanagra.query.CellValue;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.TablePointer;
import javax.annotation.Nullable;

public final class EntityMain {
  private EntityMain() {}

  public static final String TABLE_NAME = "ENT";
  public static final String ATTRIBUTE_DISPLAY_FIELD_NAME = "DISP";
  public static final CellValue.SQLDataType ATTRIBUTE_DISPLAY_SQL_TYPE =
      CellValue.SQLDataType.STRING;
  public static final String HIERARCHY_PATH_FIELD_NAME = "PATH";
  public static final CellValue.SQLDataType HIERARCHY_PATH_SQL_TYPE = CellValue.SQLDataType.STRING;
  public static final String HIERARCHY_NUMCHILDREN_FIELD_NAME = "NUMCH";
  public static final CellValue.SQLDataType HIERARCHY_NUMCHILDREN_SQL_TYPE =
      CellValue.SQLDataType.INT64;
  public static final String ENTITY_GROUP_COUNT_FIELD_NAME = "EGCT";
  public static final CellValue.SQLDataType ENTITY_GROUP_COUNT_SQL_TYPE =
      CellValue.SQLDataType.INT64;
  public static final String NO_HIERARCHY_NAME = "NOHIER";
  public static final String TEXT_COMBINED_SEARCH_FIELD_NAME = "TXT";

  public static TablePointer getTable(String entity) {
    String tableName = SchemaUtils.getSingleton().getReservedTableName(TABLE_NAME + "_" + entity);
    return SchemaUtils.getSingleton().getIndexTable(tableName);
  }

  public static FieldPointer getAttributeValueField(String entity, String attribute) {
    return new FieldPointer.Builder()
        .tablePointer(getTable(entity))
        .columnName(getAttributeValueFieldName(entity, attribute))
        .build();
  }

  public static String getAttributeValueFieldName(String entity, String attribute) {
    return attribute;
  }

  public static FieldPointer getAttributeDisplayField(String entity, String attribute) {
    return new FieldPointer.Builder()
        .columnName(getAttributeValueFieldName(entity, attribute))
        .tablePointer(getTable(entity))
        .build();
  }

  public static String getAttributeDisplayFieldName(String entity, String attribute) {
    return SchemaUtils.getReservedFieldName(ATTRIBUTE_DISPLAY_FIELD_NAME + "_" + attribute);
  }

  public static FieldPointer getHierarchyPathField(String entity, String hierarchy) {
    return new FieldPointer.Builder()
        .columnName(getHierarchyPathFieldName(entity, hierarchy))
        .tablePointer(getTable(entity))
        .build();
  }

  public static String getHierarchyPathFieldName(String entity, String hierarchy) {
    return SchemaUtils.getReservedFieldName(HIERARCHY_PATH_FIELD_NAME + "_" + hierarchy);
  }

  public static FieldPointer getHierarchyNumChildrenField(String entity, String hierarchy) {
    return new FieldPointer.Builder()
        .columnName(getHierarchyNumchildrenFieldName(entity, hierarchy))
        .tablePointer(getTable(entity))
        .build();
  }

  public static String getHierarchyNumchildrenFieldName(String entity, String hierarchy) {
    return SchemaUtils.getReservedFieldName(HIERARCHY_NUMCHILDREN_FIELD_NAME + "_" + hierarchy);
  }

  public static FieldPointer getEntityGroupCountField(
      String entity, @Nullable String hierarchy, String entityGroup) {
    return new FieldPointer.Builder()
        .columnName(getEntityGroupCountFieldName(entity, hierarchy, entityGroup))
        .tablePointer(getTable(entity))
        .build();
  }

  public static String getEntityGroupCountFieldName(
      String entity, @Nullable String hierarchy, String entityGroup) {
    return SchemaUtils.getReservedFieldName(
        ENTITY_GROUP_COUNT_FIELD_NAME
            + "_"
            + entityGroup
            + "_"
            + (hierarchy == null ? NO_HIERARCHY_NAME : hierarchy));
  }

  public static FieldPointer getTextCombinedSearchField(String entity) {
    String fieldName = SchemaUtils.getReservedFieldName(TEXT_COMBINED_SEARCH_FIELD_NAME);
    return new FieldPointer.Builder().columnName(fieldName).tablePointer(getTable(entity)).build();
  }
}
