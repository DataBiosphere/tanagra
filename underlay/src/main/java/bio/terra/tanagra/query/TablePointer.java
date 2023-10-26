package bio.terra.tanagra.query;

import bio.terra.tanagra.exception.InvalidConfigException;
import bio.terra.tanagra.serialization.UFTablePointer;
import bio.terra.tanagra.utils.FileIO;
import bio.terra.tanagra.utils.FileUtils;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class TablePointer implements SQLExpression {
  private static final String SQL_DIRECTORY_NAME = "sql";

  private final DataPointer dataPointer;
  private final String tableName;
  private final Filter filter;
  private final String sql;

  private TablePointer(Builder builder) {
    this.dataPointer = builder.dataPointer;
    this.tableName = builder.tableName;
    this.filter = builder.filter;
    this.sql = builder.sql;
  }

  public static TablePointer fromTableName(String tableName, DataPointer dataPointer) {
    return new Builder().dataPointer(dataPointer).tableName(tableName).build();
  }

  public static TablePointer fromRawSql(String sql, DataPointer dataPointer) {
    return new Builder().dataPointer(dataPointer).sql(sql).build();
  }

  public static TablePointer fromSerialized(UFTablePointer serialized, DataPointer dataPointer) {
    if (!Strings.isNullOrEmpty(serialized.getRawSql())) {
      // Table is defined by a raw SQL string, which is specified directly in the JSON.
      return TablePointer.fromRawSql(serialized.getRawSql(), dataPointer);
    } else if (!Strings.isNullOrEmpty(serialized.getRawSqlFile())) {
      // Table is defined by a raw SQL string, which is in a file path that is specified in the
      // JSON.
      Path rawSqlFile =
          FileIO.getInputParentDir()
              .resolve(SQL_DIRECTORY_NAME)
              .resolve(Path.of(serialized.getRawSqlFile()));
      String rawSqlString =
          FileUtils.readStringFromFileNoLineBreaks(
              FileIO.getGetFileInputStreamFunction().apply(rawSqlFile));
      return TablePointer.fromRawSql(rawSqlString, dataPointer);
    }
    // Table is defined by a table name and optional filter.

    if (Strings.isNullOrEmpty(serialized.getTable())) {
      throw new InvalidConfigException("Table name not defined");
    }

    TablePointer tablePointer = TablePointer.fromTableName(serialized.getTable(), dataPointer);
    if (serialized.getFilter() == null) {
      return tablePointer;
    } else {
      Filter filter = serialized.getFilter().deserializeToInternal(tablePointer);
      return new Builder()
          .dataPointer(dataPointer)
          .tableName(serialized.getTable())
          .tableFilter(filter)
          .build();
    }
  }

  public DataPointer getDataPointer() {
    return dataPointer;
  }

  public String getTableName() {
    return tableName;
  }

  public boolean hasTableFilter() {
    return filter != null;
  }

  public Filter getTableFilter() {
    return filter;
  }

  public boolean isRawSql() {
    return sql != null;
  }

  public String getSql() {
    return sql;
  }

  @Override
  public String renderSQL() {
    if (isRawSql()) {
      return "(" + sql + ")";
    } else if (!hasTableFilter()) {
      return dataPointer.getTableSQL(tableName);
    } else {
      TablePointer tablePointerWithoutFilter = TablePointer.fromTableName(tableName, dataPointer);
      TableVariable tableVar = TableVariable.forPrimary(tablePointerWithoutFilter);
      FieldVariable fieldVar =
          new FieldVariable(FieldPointer.allFields(tablePointerWithoutFilter), tableVar);
      FilterVariable filterVar = getTableFilter().buildVariable(tableVar, List.of(tableVar));

      Query query =
          new Query.Builder()
              .select(List.of(fieldVar))
              .tables(List.of(tableVar))
              .where(filterVar)
              .build();
      return "(" + query.renderSQL() + ")";
    }
  }

  public String getPathForIndexing() {
    return dataPointer.getTablePathForIndexing(tableName);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TablePointer that = (TablePointer) o;
    return dataPointer.equals(that.dataPointer)
        && Objects.equals(tableName, that.tableName)
        && Objects.equals(filter, that.filter)
        && Objects.equals(sql, that.sql);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dataPointer, tableName, filter, sql);
  }

  public static class Builder {
    private DataPointer dataPointer;
    private String tableName;
    private Filter filter;
    private String sql;

    public Builder dataPointer(DataPointer dataPointer) {
      this.dataPointer = dataPointer;
      return this;
    }

    public Builder tableName(String tableName) {
      this.tableName = tableName;
      return this;
    }

    public Builder tableFilter(Filter filter) {
      this.filter = filter;
      return this;
    }

    public Builder sql(String sql) {
      this.sql = sql;
      return this;
    }

    /** Call the private constructor. */
    public TablePointer build() {
      return new TablePointer(this);
    }
  }
}
