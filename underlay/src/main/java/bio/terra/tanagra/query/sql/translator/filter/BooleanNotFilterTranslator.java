package bio.terra.tanagra.query.sql.translator.filter;

import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;

public class BooleanNotFilterTranslator extends ApiFilterTranslator {
  private final ApiFilterTranslator subFilterTranslator;

  public BooleanNotFilterTranslator(
      ApiTranslator apiTranslator, BooleanNotFilter booleanNotFilter) {
    super(apiTranslator);
    this.subFilterTranslator = apiTranslator.translator(booleanNotFilter.getSubFilter());
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    return apiTranslator.booleanNotFilterSql(subFilterTranslator.buildSql(sqlParams, tableAlias));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return subFilterTranslator.isFilterOnAttribute(attribute);
  }

  @Override
  public ApiFilterTranslator swapAttributeField(Attribute attribute, SqlField swappedField) {
    subFilterTranslator.swapAttributeField(attribute, swappedField);
    return this;
  }
}
