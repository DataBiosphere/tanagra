package bio.terra.tanagra.query;

/** The schema for a column in a {@link RowResult} describing the data in a column. */
public class ColumnSchema {
  private final String columnName;
  private final CellValue.SQLDataType sqlDataType;
  private final boolean isRequired;

  public ColumnSchema(String columnName, CellValue.SQLDataType sqlDataType) {
    this(columnName, sqlDataType, false);
  }

  public ColumnSchema(String columnName, CellValue.SQLDataType sqlDataType, boolean isRequired) {
    this.columnName = columnName;
    this.sqlDataType = sqlDataType;
    this.isRequired = isRequired;
  }

  public ColumnSchema(FieldPointer fieldPointer, CellValue.SQLDataType sqlDataType) {
    this.columnName = fieldPointer.getColumnName();
    this.sqlDataType = sqlDataType;
    this.isRequired = false;
  }

  public String getColumnName() {
    return columnName;
  }

  public CellValue.SQLDataType getSqlDataType() {
    return sqlDataType;
  }

  public boolean isRequired() {
    return isRequired;
  }
}
