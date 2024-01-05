package bio.terra.tanagra.query.sql.translator;

import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import java.util.HashMap;
import java.util.Map;

public abstract class ApiFilterTranslator {
  protected final ApiTranslator apiTranslator;
  protected final Map<Attribute, SqlField> attributeSwapFields = new HashMap<>();

  protected ApiFilterTranslator(ApiTranslator apiTranslator) {
    this.apiTranslator = apiTranslator;
  }

  public abstract String buildSql(SqlParams sqlParams, String tableAlias);

  public abstract boolean isFilterOnAttribute(Attribute attribute);

  public ApiFilterTranslator swapAttributeField(Attribute attribute, SqlField swappedField) {
    attributeSwapFields.put(attribute, swappedField);
    return this;
  }
}
