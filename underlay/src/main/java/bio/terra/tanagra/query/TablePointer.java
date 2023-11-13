package bio.terra.tanagra.query;

import java.util.List;
import java.util.Objects;

public final class TablePointer implements SQLExpression {
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
