package bio.terra.tanagra.query.sql;

public abstract class SqlTable {
  protected final String tableName;
  protected final String sql;

  protected SqlTable(String tableName, String sql) {
    this.tableName = tableName;
    this.sql = sql;
  }

  public String getTableName() {
    return tableName;
  }

  public boolean isRawSql() {
    return sql != null;
  }

  public String getSql() {
    return sql;
  }

  public abstract String render();
}
