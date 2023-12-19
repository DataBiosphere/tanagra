package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.BooleanNotFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlGeneration;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;

public class BQBooleanNotFilterTranslator implements SqlFilterTranslator {
  private final BooleanNotFilter booleanNotFilter;

  public BQBooleanNotFilterTranslator(BooleanNotFilter booleanNotFilter) {
    this.booleanNotFilter = booleanNotFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    return SqlGeneration.booleanNotFilterSql(
        BQTranslator.translator(booleanNotFilter.getSubFilter())
            .buildSql(sqlParams, tableAlias, idField));
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return BQTranslator.translator(booleanNotFilter).isFilterOnAttribute(attribute);
  }
}
