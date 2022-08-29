package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFFieldPointer;
import com.google.common.base.Strings;
import java.util.List;

public class FieldPointer {
  private static final String ALL_FIELDS_COLUMN_NAME = "*";

  private final TablePointer tablePointer;
  private final String columnName;
  private TablePointer foreignTablePointer;
  private String foreignKeyColumnName;
  private String foreignColumnName;
  private String sqlFunctionWrapper;

  public FieldPointer(TablePointer tablePointer, String columnName) {
    this.tablePointer = tablePointer;
    this.columnName = columnName;
  }

  public FieldPointer(TablePointer tablePointer, String columnName, String sqlFunctionWrapper) {
    this.tablePointer = tablePointer;
    this.columnName = columnName;
    this.sqlFunctionWrapper = sqlFunctionWrapper;
  }

  private FieldPointer(
      TablePointer tablePointer,
      String columnName,
      String sqlFunctionWrapper,
      TablePointer foreignTablePointer,
      String foreignKeyColumnName,
      String foreignColumnName) {
    this.tablePointer = tablePointer;
    this.columnName = columnName;
    this.sqlFunctionWrapper = sqlFunctionWrapper;
    this.foreignTablePointer = foreignTablePointer;
    this.foreignKeyColumnName = foreignKeyColumnName;
    this.foreignColumnName = foreignColumnName;
  }

  public static FieldPointer allFields(TablePointer tablePointer) {
    return new FieldPointer(tablePointer, ALL_FIELDS_COLUMN_NAME);
  }

  public static FieldPointer fromSerialized(UFFieldPointer serialized, TablePointer tablePointer) {
    boolean foreignTableDefined = !Strings.isNullOrEmpty(serialized.getForeignTable());
    boolean foreignKeyColumnDefined = !Strings.isNullOrEmpty(serialized.getForeignKey());
    boolean foreignColumnDefined = !Strings.isNullOrEmpty(serialized.getForeignColumn());
    boolean allForeignKeyFieldsDefined =
        foreignTableDefined && foreignKeyColumnDefined && foreignColumnDefined;
    boolean noForeignKeyFieldsDefined =
        !foreignTableDefined && !foreignKeyColumnDefined && !foreignColumnDefined;

    if (noForeignKeyFieldsDefined) {
      return new FieldPointer(
          tablePointer, serialized.getColumn(), serialized.getSqlFunctionWrapper());
    } else if (allForeignKeyFieldsDefined) {
      // assume the foreign table is part of the same data pointer as the original table
      TablePointer foreignTablePointer =
          new TablePointer(serialized.getForeignTable(), tablePointer.getDataPointer());
      return new FieldPointer(
          tablePointer,
          serialized.getColumn(),
          serialized.getSqlFunctionWrapper(),
          foreignTablePointer,
          serialized.getForeignKey(),
          serialized.getForeignColumn());
    } else {
      throw new IllegalArgumentException("Only some foreign key fields are defined");
    }
  }

  public FieldVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tableVariables) {
    return buildVariable(primaryTable, tableVariables, null);
  }

  public FieldVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tableVariables, String alias) {
    if (isForeignKey()) {
      FieldVariable primaryTableColumn =
          new FieldVariable(new FieldPointer(tablePointer, columnName), primaryTable);
      TableVariable foreignTable =
          TableVariable.forJoined(foreignTablePointer, foreignKeyColumnName, primaryTableColumn);
      tableVariables.add(foreignTable);
      return new FieldVariable(
          new FieldPointer(foreignTablePointer, foreignColumnName), foreignTable, alias);
    } else {
      return new FieldVariable(this, primaryTable, alias);
    }
  }

  public boolean isForeignKey() {
    return foreignTablePointer != null;
  }

  public String getColumnName() {
    return columnName;
  }

  public TablePointer getForeignTablePointer() {
    return foreignTablePointer;
  }

  public String getForeignKeyColumnName() {
    return foreignKeyColumnName;
  }

  public String getForeignColumnName() {
    return foreignColumnName;
  }

  public boolean hasSqlFunctionWrapper() {
    return sqlFunctionWrapper != null;
  }

  public String getSqlFunctionWrapper() {
    return sqlFunctionWrapper;
  }

  public TablePointer getTablePointer() {
    return tablePointer;
  }

  public boolean isAllFields() {
    return ALL_FIELDS_COLUMN_NAME.equals(columnName);
  }
}
