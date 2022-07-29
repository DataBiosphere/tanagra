package bio.terra.tanagra.underlay;

import bio.terra.tanagra.serialization.UFFieldPointer;
import com.google.common.base.Strings;

public class FieldPointer {
  private TablePointer tablePointer;
  private String columnName;
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

  public static FieldPointer fromSerialized(UFFieldPointer serialized, TablePointer tablePointer) {
    boolean foreignTableDefined = Strings.isNullOrEmpty(serialized.foreignTable);
    boolean foreignKeyColumnDefined = Strings.isNullOrEmpty(serialized.foreignKey);
    boolean foreignColumnDefined = Strings.isNullOrEmpty(serialized.foreignColumn);
    boolean allForeignKeyFieldsDefined =
        foreignTableDefined && foreignKeyColumnDefined && foreignColumnDefined;
    boolean noForeignKeyFieldsDefined =
        !foreignTableDefined && !foreignKeyColumnDefined && !foreignColumnDefined;

    if (noForeignKeyFieldsDefined) {
      return new FieldPointer(tablePointer, serialized.column, serialized.sqlFunctionWrapper);
    } else if (allForeignKeyFieldsDefined) {
      // assume the foreign table is part of the same data pointer as the original table
      TablePointer foreignTablePointer =
          new TablePointer(serialized.foreignTable, tablePointer.getDataPointer());
      return new FieldPointer(
          tablePointer,
          serialized.column,
          serialized.sqlFunctionWrapper,
          foreignTablePointer,
          serialized.foreignKey,
          serialized.foreignColumn);
    } else {
      throw new IllegalArgumentException("Only some foreign key fields are defined");
    }
  }
}
