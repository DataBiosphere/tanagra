package bio.terra.tanagra.query;

import java.io.Serializable;
import java.util.Objects;

/** The schema for a column in a {@link RowResult} describing the data in a column. */
public class ColumnSchema implements Serializable {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ColumnSchema that = (ColumnSchema) o;
    return isRequired == that.isRequired
        && columnName.equals(that.columnName)
        && sqlDataType == that.sqlDataType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnName, sqlDataType, isRequired);
  }
}
