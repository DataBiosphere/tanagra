package bio.terra.tanagra.query2.sql;

import java.util.Objects;

public class SqlField {
  private final SqlTable sqlTable;
  private final String columnName;
  private final String sqlFunctionWrapper;

  private SqlField(Builder builder) {
    this.sqlTable = builder.sqlTable;
    this.columnName = builder.columnName;
    this.sqlFunctionWrapper = builder.sqlFunctionWrapper;
  }

  public Builder toBuilder() {
    return new Builder()
        .tablePointer(sqlTable)
        .columnName(columnName)
        .sqlFunctionWrapper(sqlFunctionWrapper);
  }

  public String getColumnName() {
    return columnName;
  }

  public boolean hasSqlFunctionWrapper() {
    return sqlFunctionWrapper != null;
  }

  public String getSqlFunctionWrapper() {
    return sqlFunctionWrapper;
  }

  public SqlTable getTablePointer() {
    return sqlTable;
  }

  public static class Builder {
    private SqlTable sqlTable;
    private String columnName;
    private String sqlFunctionWrapper;

    public Builder tablePointer(SqlTable sqlTable) {
      this.sqlTable = sqlTable;
      return this;
    }

    public Builder columnName(String columnName) {
      this.columnName = columnName;
      return this;
    }

    public Builder sqlFunctionWrapper(String sqlFunctionWrapper) {
      this.sqlFunctionWrapper = sqlFunctionWrapper;
      return this;
    }

    public SqlField build() {
      return new SqlField(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SqlField that = (SqlField) o;
    return sqlTable.equals(that.sqlTable)
        && columnName.equals(that.columnName)
        && Objects.equals(sqlFunctionWrapper, that.sqlFunctionWrapper);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sqlTable, columnName, sqlFunctionWrapper);
  }
}
