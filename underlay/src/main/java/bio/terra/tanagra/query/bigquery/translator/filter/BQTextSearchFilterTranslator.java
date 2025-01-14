package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.api.shared.Literal;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.Map;

public class BQTextSearchFilterTranslator extends ApiFilterTranslator {
  private final TextSearchFilter textSearchFilter;

  public BQTextSearchFilterTranslator(
      ApiTranslator apiTranslator,
      TextSearchFilter textSearchFilter,
      Map<Attribute, SqlField> attributeSwapFields) {
    super(apiTranslator, attributeSwapFields);
    this.textSearchFilter = textSearchFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    ITEntityMain indexTable =
        textSearchFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(textSearchFilter.getEntity().getName());
    SqlField textSearchField;
    if (textSearchFilter.isForSpecificAttribute()) {
      // Search only on the specified attribute.
      textSearchField = fetchSelectField(indexTable, textSearchFilter.getAttribute());
    } else {
      // Search the text index specified in the underlay config.
      textSearchField = indexTable.getTextSearchField();
    }
    return apiTranslator.textSearchFilterSql(
        textSearchField,
        textSearchFilter.getOperator(),
        Literal.forString(textSearchFilter.getText()),
        tableAlias,
        sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return textSearchFilter.isForSpecificAttribute()
        && textSearchFilter.getAttribute().equals(attribute);
  }
}
