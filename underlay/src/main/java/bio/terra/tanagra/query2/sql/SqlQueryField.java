package bio.terra.tanagra.query2.sql;

public final class SqlQueryField {
  private final SqlField field;
  private final String alias;

  private SqlQueryField(SqlField field, String alias) {
    this.field = field;
    this.alias = alias;
  }

  public static SqlQueryField of(SqlField field, String alias) {
    return new SqlQueryField(field, alias);
  }

  public static SqlQueryField of(SqlField field) {
    return of(field, null);
  }

  public SqlField getField() {
    return field;
  }

  public String getAlias() {
    return alias;
  }
}
