package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.query.FieldPointer;

public final class SqlField {
  private final FieldPointer field;
  private final String alias;

  private SqlField(FieldPointer field, String alias) {
    this.field = field;
    this.alias = alias;
  }

  public static SqlField of(FieldPointer field, String alias) {
    return new SqlField(field, alias);
  }

  public FieldPointer getField() {
    return field;
  }

  public String getAlias() {
    return alias;
  }
}
