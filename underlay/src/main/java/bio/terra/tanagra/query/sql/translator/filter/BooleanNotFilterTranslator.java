package bio.terra.tanagra.query.sql.translator.filter;

import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;

public class BooleanNotFilterTranslator extends ApiFilterTranslator {
  private final BooleanNotFilter booleanNotFilter;

  public BooleanNotFilterTranslator(
          ApiTranslator apiTranslator, BooleanNotFilter booleanNotFilter) {
    super(apiTranslator);
    this.booleanNotFilter = booleanNotFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    return apiTranslator.booleanNotFilterSql(
        apiTranslator.translator(booleanNotFilter.getSubFilter()).buildSql(sqlParams, tableAlias));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return apiTranslator.translator(booleanNotFilter).isFilterOnAttribute(attribute);
  }
}
