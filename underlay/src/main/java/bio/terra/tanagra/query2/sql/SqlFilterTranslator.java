package bio.terra.tanagra.query2.sql;

import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.underlay.entitymodel.Attribute;

import java.util.HashMap;
import java.util.Map;

public abstract class SqlFilterTranslator {
  protected final SqlTranslator sqlTranslator;
  protected final Map<Attribute, FieldPointer> attributeSwapFields = new HashMap<>();

  protected SqlFilterTranslator(SqlTranslator sqlTranslator) {
    this.sqlTranslator = sqlTranslator;
  }

  public abstract String buildSql(SqlParams sqlParams, String tableAlias);

  public abstract boolean isFilterOnAttribute(Attribute attribute);

  public SqlFilterTranslator swapAttributeField(Attribute attribute, FieldPointer swappedField) {
    attributeSwapFields.put(attribute, swappedField);
    return this;
  }
}
