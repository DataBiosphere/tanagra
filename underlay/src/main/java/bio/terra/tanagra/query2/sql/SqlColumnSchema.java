package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.api.shared.DataType;
import java.io.Serializable;
import java.util.Objects;

public class SqlColumnSchema implements Serializable {
  private final String columnName;
  private final DataType dataType;
  private final boolean isRequired;

  public SqlColumnSchema(String columnName, DataType dataType) {
    this(columnName, dataType, false);
  }

  public SqlColumnSchema(String columnName, DataType dataType, boolean isRequired) {
    this.columnName = columnName;
    this.dataType = dataType;
    this.isRequired = isRequired;
  }

  public String getColumnName() {
    return columnName;
  }

  public DataType getDataType() {
    return dataType;
  }

  public boolean isRequired() {
    return isRequired;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SqlColumnSchema that = (SqlColumnSchema) o;
    return isRequired == that.isRequired
        && columnName.equals(that.columnName)
        && dataType == that.dataType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnName, dataType, isRequired);
  }
}
