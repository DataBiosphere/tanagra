package bio.terra.tanagra.underlay;

import bio.terra.tanagra.api.shared.DataType;
import java.io.Serializable;
import java.util.Objects;

public class ColumnSchema implements Serializable {
  private final String columnName;
  private final DataType dataType;
  private final boolean isDataTypeRepeated;
  private final boolean isRequired;

  public ColumnSchema(String columnName, DataType dataType) {
    this(columnName, dataType, false, false);
  }

  public ColumnSchema(String columnName, DataType dataType, boolean isRequired) {
    this(columnName, dataType, false, isRequired);
  }

  public ColumnSchema(
      String columnName, DataType dataType, boolean isDataTypeRepeated, boolean isRequired) {
    this.columnName = columnName;
    this.dataType = dataType;
    this.isDataTypeRepeated = isDataTypeRepeated;
    this.isRequired = isRequired;
  }

  public String getColumnName() {
    return columnName;
  }

  public DataType getDataType() {
    return dataType;
  }

  public boolean isDataTypeRepeated() {
    return isDataTypeRepeated;
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
    ColumnSchema that = (ColumnSchema) o;
    return isRequired == that.isRequired
        && columnName.equals(that.columnName)
        && dataType == that.dataType
        && isDataTypeRepeated == that.isDataTypeRepeated;
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnName, dataType, isDataTypeRepeated, isRequired);
  }
}
