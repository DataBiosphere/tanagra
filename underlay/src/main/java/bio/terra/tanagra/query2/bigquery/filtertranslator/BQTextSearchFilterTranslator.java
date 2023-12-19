package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.TextSearchFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query.Literal;
import bio.terra.tanagra.query2.bigquery.BQTranslator;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlGeneration;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import java.util.List;

public class BQTextSearchFilterTranslator implements SqlFilterTranslator {
  private final TextSearchFilter textSearchFilter;

  public BQTextSearchFilterTranslator(TextSearchFilter textSearchFilter) {
    this.textSearchFilter = textSearchFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    ITEntityMain indexTable =
        textSearchFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(textSearchFilter.getEntity().getName());
    FieldPointer textSearchField;
    if (textSearchFilter.isForSpecificAttribute()) {
      // Search only on the specified attribute.
      Attribute attribute = textSearchFilter.getAttribute();
      textSearchField =
          attribute.isId() ? idField : indexTable.getAttributeValueField(attribute.getName());
      if (attribute.hasRuntimeSqlFunctionWrapper()) {
        textSearchField =
            textSearchField
                .toBuilder()
                .sqlFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper())
                .build();
      }
    } else {
      // Search the text index specified in the underlay config.
      textSearchField = indexTable.getTextSearchField();
    }
    return SqlGeneration.functionFilterSql(
        textSearchField,
        BQTranslator.functionTemplateSql(textSearchFilter.getFunctionTemplate()),
        List.of(new Literal(textSearchFilter.getText())),
        tableAlias,
        sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return textSearchFilter.isForSpecificAttribute()
        && textSearchFilter.getAttribute().equals(attribute);
  }
}
