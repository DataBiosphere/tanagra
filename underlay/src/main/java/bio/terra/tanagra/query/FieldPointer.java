package bio.terra.tanagra.query;

import java.util.List;
import java.util.Objects;

public class FieldPointer {
  private static final String ALL_FIELDS_COLUMN_NAME = "*";

  private final TablePointer tablePointer;
  private final String columnName;
  private final TablePointer foreignTablePointer;
  private final String foreignKeyColumnName;
  private final String foreignColumnName;
  private boolean joinCanBeEmpty;
  private final String sqlFunctionWrapper;
  private final boolean runtimeCalculated;

  private FieldPointer(Builder builder) {
    this.tablePointer = builder.tablePointer;
    this.columnName = builder.columnName;
    this.foreignTablePointer = builder.foreignTablePointer;
    this.foreignKeyColumnName = builder.foreignKeyColumnName;
    this.foreignColumnName = builder.foreignColumnName;
    this.joinCanBeEmpty = builder.joinCanBeEmpty;
    this.sqlFunctionWrapper = builder.sqlFunctionWrapper;
    this.runtimeCalculated = builder.runtimeCalculated;
  }

  public static FieldPointer allFields(TablePointer tablePointer) {
    return new Builder().tablePointer(tablePointer).columnName(ALL_FIELDS_COLUMN_NAME).build();
  }

  public FieldVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tableVariables) {
    return buildVariable(primaryTable, tableVariables, null);
  }

  public FieldVariable buildVariable(
      TableVariable primaryTable, List<TableVariable> tableVariables, String alias) {
    return buildVariable(primaryTable, tableVariables, alias, false);
  }

  public FieldVariable buildVariableForIndexing(
      TableVariable primaryTable, List<TableVariable> tableVariables, String alias) {
    return buildVariable(primaryTable, tableVariables, alias, true);
  }

  public FieldVariable buildVariable(
      TableVariable primaryTable,
      List<TableVariable> tableVariables,
      String alias,
      boolean forIndexing) {
    if (isForeignKey()) {
      FieldVariable primaryTableColumn =
          new FieldVariable(
              new Builder().tablePointer(tablePointer).columnName(columnName).build(),
              primaryTable);
      TableVariable foreignTable =
          joinCanBeEmpty
              ? TableVariable.forLeftJoined(
                  foreignTablePointer, foreignKeyColumnName, primaryTableColumn)
              : TableVariable.forJoined(
                  foreignTablePointer, foreignKeyColumnName, primaryTableColumn);
      // TODO: Check if there is already a table variable with the same JOIN criteria, so we don't
      // JOIN the same table for each field we need from it.
      tableVariables.add(foreignTable);
      return new FieldVariable(
          new Builder()
              .tablePointer(foreignTablePointer)
              .columnName(foreignColumnName)
              .sqlFunctionWrapper(forIndexing && runtimeCalculated ? null : sqlFunctionWrapper)
              .build(),
          foreignTable,
          alias);
    } else {
      return forIndexing && runtimeCalculated
          ? new FieldVariable(
              this.toBuilder().runtimeCalculated(false).sqlFunctionWrapper(null).build(),
              primaryTable,
              alias)
          : new FieldVariable(this, primaryTable, alias);
    }
  }

  public Builder toBuilder() {
    return new Builder()
        .tablePointer(tablePointer)
        .columnName(columnName)
        .foreignTablePointer(foreignTablePointer)
        .foreignKeyColumnName(foreignKeyColumnName)
        .foreignColumnName(foreignColumnName)
        .joinCanBeEmpty(joinCanBeEmpty)
        .sqlFunctionWrapper(sqlFunctionWrapper);
  }

  public boolean isForeignKey() {
    return foreignTablePointer != null;
  }

  public String getColumnName() {
    return columnName;
  }

  public TablePointer getForeignTablePointer() {
    return foreignTablePointer;
  }

  public String getForeignKeyColumnName() {
    return foreignKeyColumnName;
  }

  public String getForeignColumnName() {
    return foreignColumnName;
  }

  public FieldPointer setJoinCanBeEmpty(boolean joinCanBeEmpty) {
    this.joinCanBeEmpty = joinCanBeEmpty;
    return this;
  }

  public boolean hasSqlFunctionWrapper() {
    return sqlFunctionWrapper != null;
  }

  public String getSqlFunctionWrapper() {
    return sqlFunctionWrapper;
  }

  public boolean isRuntimeCalculated() {
    return runtimeCalculated;
  }

  public TablePointer getTablePointer() {
    return tablePointer;
  }

  public static class Builder {
    private TablePointer tablePointer;
    private String columnName;
    private TablePointer foreignTablePointer;
    private String foreignKeyColumnName;
    private String foreignColumnName;
    private boolean joinCanBeEmpty;
    private String sqlFunctionWrapper;
    private boolean runtimeCalculated;

    public Builder tablePointer(TablePointer tablePointer) {
      this.tablePointer = tablePointer;
      return this;
    }

    public Builder columnName(String columnName) {
      this.columnName = columnName;
      return this;
    }

    public Builder foreignTablePointer(TablePointer foreignTablePointer) {
      this.foreignTablePointer = foreignTablePointer;
      return this;
    }

    public Builder foreignKeyColumnName(String foreignKeyColumnName) {
      this.foreignKeyColumnName = foreignKeyColumnName;
      return this;
    }

    public Builder foreignColumnName(String foreignColumnName) {
      this.foreignColumnName = foreignColumnName;
      return this;
    }

    public Builder joinCanBeEmpty(boolean joinCanBeEmpty) {
      this.joinCanBeEmpty = joinCanBeEmpty;
      return this;
    }

    public Builder sqlFunctionWrapper(String sqlFunctionWrapper) {
      this.sqlFunctionWrapper = sqlFunctionWrapper;
      return this;
    }

    public Builder runtimeCalculated(boolean runtimeCalculated) {
      this.runtimeCalculated = runtimeCalculated;
      return this;
    }

    /** Call the private constructor. */
    public FieldPointer build() {
      return new FieldPointer(this);
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
    FieldPointer that = (FieldPointer) o;
    return joinCanBeEmpty == that.joinCanBeEmpty
        && runtimeCalculated == that.runtimeCalculated
        && tablePointer.equals(that.tablePointer)
        && columnName.equals(that.columnName)
        && Objects.equals(foreignTablePointer, that.foreignTablePointer)
        && Objects.equals(foreignKeyColumnName, that.foreignKeyColumnName)
        && Objects.equals(foreignColumnName, that.foreignColumnName)
        && Objects.equals(sqlFunctionWrapper, that.sqlFunctionWrapper);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        tablePointer,
        columnName,
        foreignTablePointer,
        foreignKeyColumnName,
        foreignColumnName,
        joinCanBeEmpty,
        sqlFunctionWrapper,
        runtimeCalculated);
  }
}
