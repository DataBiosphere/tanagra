package bio.terra.tanagra.query2.sql.filtertranslator;

import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.query2.sql.SqlTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;

public class BooleanNotFilterTranslator extends SqlFilterTranslator {
  private final BooleanNotFilter booleanNotFilter;

  public BooleanNotFilterTranslator(
      SqlTranslator sqlTranslator, BooleanNotFilter booleanNotFilter) {
    super(sqlTranslator);
    this.booleanNotFilter = booleanNotFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    return sqlTranslator.booleanNotFilterSql(
        sqlTranslator.translator(booleanNotFilter.getSubFilter()).buildSql(sqlParams, tableAlias));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return sqlTranslator.translator(booleanNotFilter).isFilterOnAttribute(attribute);
  }
}
