package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.entitymodel.Attribute;

public abstract class SqlFilterTranslator {
  protected final SqlTranslator sqlTranslator;

  protected SqlFilterTranslator(SqlTranslator sqlTranslator) {
    this.sqlTranslator = sqlTranslator;
  }

  public abstract String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField);

  public abstract boolean isFilterOnAttribute(Attribute attribute);
}
