package bio.terra.tanagra.underlay;

import bio.terra.tanagra.query.FieldVariable;
import bio.terra.tanagra.query.FilterVariable;
import bio.terra.tanagra.query.Query;
import bio.terra.tanagra.query.SQLExpression;
import bio.terra.tanagra.query.TableVariable;
import bio.terra.tanagra.serialization.UFTablePointer;
import com.google.common.base.Strings;
import java.util.List;

public final class TablePointer implements SQLExpression {
  private final DataPointer dataPointer;
  private final String tableName;
  private final TableFilter tableFilter;
  private final String sql;

  private TablePointer(Builder builder) {
    this.dataPointer = builder.dataPointer;
    this.tableName = builder.tableName;
    this.tableFilter = builder.tableFilter;
    this.sql = builder.sql;
  }

  public static TablePointer fromTableName(String tableName, DataPointer dataPointer) {
    return new Builder().dataPointer(dataPointer).tableName(tableName).build();
  }

  public static TablePointer fromRawSql(String sql, DataPointer dataPointer) {
    return new Builder().dataPointer(dataPointer).sql(sql).build();
  }

  public static TablePointer fromSerialized(UFTablePointer serialized, DataPointer dataPointer) {
    if (Strings.isNullOrEmpty(serialized.getTable())) {
      throw new IllegalArgumentException("Table name not defined");
    }

    TablePointer tablePointer = TablePointer.fromTableName(serialized.getTable(), dataPointer);
    if (serialized.getFilter() == null) {
      return tablePointer;
    } else {
      TableFilter tableFilter = serialized.getFilter().deserializeToInternal(tablePointer);
      return new Builder()
          .dataPointer(dataPointer)
          .tableName(serialized.getTable())
          .tableFilter(tableFilter)
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
    return tableFilter != null;
  }

  public TableFilter getTableFilter() {
    return tableFilter;
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

  public FilterVariable getFilterVariable(TableVariable tableVariable, List<TableVariable> tables) {
    if (!hasTableFilter()) {
      return null;
    }
    return getTableFilter().buildVariable(tableVariable, tables);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TablePointer)) {
      return false;
    }

    TablePointer objTP = (TablePointer) obj;
    return objTP.getDataPointer().equals(getDataPointer())
        && objTP.getTableName().equals(getTableName())
        && ((!objTP.hasTableFilter() && !hasTableFilter())
            || (objTP.hasTableFilter()) && objTP.getTableFilter().equals(getTableFilter()));
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 37 * hash + (this.dataPointer != null ? this.dataPointer.hashCode() : 0);
    hash = 37 * hash + (this.tableName != null ? this.tableName.hashCode() : 0);
    hash = 37 * hash + (this.tableFilter != null ? this.tableFilter.hashCode() : 0);
    return hash;
  }

  public static class Builder {
    private DataPointer dataPointer;
    private String tableName;
    private TableFilter tableFilter;
    private String sql;

    public Builder dataPointer(DataPointer dataPointer) {
      this.dataPointer = dataPointer;
      return this;
    }

    public Builder tableName(String tableName) {
      this.tableName = tableName;
      return this;
    }

    public Builder tableFilter(TableFilter tableFilter) {
      this.tableFilter = tableFilter;
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
