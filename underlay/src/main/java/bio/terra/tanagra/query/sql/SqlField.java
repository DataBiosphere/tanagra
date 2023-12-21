package bio.terra.tanagra.query.sql;

import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SqlField {
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlField.class);
  private final String columnName;
  private final @Nullable String functionWrapper;

  private SqlField(String columnName, @Nullable String functionWrapper) {
    this.columnName = columnName;
    this.functionWrapper = functionWrapper;
  }

  public static SqlField of(String columnName, @Nullable String functionWrapper) {
    return new SqlField(columnName, functionWrapper);
  }

  public static SqlField of(String columnName) {
    return of(columnName, null);
  }

  public SqlField cloneWithFunctionWrapper(String functionWrapper) {
    if (hasFunctionWrapper()) {
      LOGGER.warn(
          "Field pointer clone is dropping the existing function wrapper ({}) and replacing it with a new one ({})",
          this.functionWrapper,
          functionWrapper);
    }
    return new SqlField(columnName, functionWrapper);
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
}
