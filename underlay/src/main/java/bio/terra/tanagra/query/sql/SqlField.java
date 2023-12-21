package bio.terra.tanagra.query.sql;

import java.util.Objects;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SqlField {
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlField.class);
  private final SqlTable table;
  private final String columnName;
  private final @Nullable String functionWrapper;

  private SqlField(SqlTable table, String columnName, @Nullable String functionWrapper) {
    this.table = table;
    this.columnName = columnName;
    this.functionWrapper = functionWrapper;
  }

  public static SqlField of(SqlTable table, String columnName, @Nullable String functionWrapper) {
    return new SqlField(table, columnName, functionWrapper);
  }

  public static SqlField of(SqlTable table, String columnName) {
    return of(table, columnName, null);
  }

  public SqlField cloneWithFunctionWrapper(String functionWrapper) {
    if (hasFunctionWrapper()) {
      LOGGER.warn(
          "Field pointer clone is dropping the existing function wrapper ({}) and replacing it with a new one ({})",
          this.functionWrapper,
          functionWrapper);
    }
    return new SqlField(table, columnName, functionWrapper);
  }

  public String getColumnName() {
    return columnName;
  }

  public boolean hasFunctionWrapper() {
    return functionWrapper != null;
  }

  public String getFunctionWrapper() {
    return functionWrapper;
  }

  public SqlTable getTable() {
    return table;
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
    return table.equals(that.table)
        && columnName.equals(that.columnName)
        && Objects.equals(functionWrapper, that.functionWrapper);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, columnName, functionWrapper);
  }
}
