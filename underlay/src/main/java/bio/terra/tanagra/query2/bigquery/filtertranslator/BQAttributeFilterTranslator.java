package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.query2.sql.ApiFilterTranslator;
import bio.terra.tanagra.query2.sql.ApiTranslator;
import bio.terra.tanagra.query2.sql.SqlField;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;

public class BQAttributeFilterTranslator extends ApiFilterTranslator {
  private final AttributeFilter attributeFilter;

  public BQAttributeFilterTranslator(ApiTranslator apiTranslator, AttributeFilter attributeFilter) {
    super(apiTranslator);
    this.attributeFilter = attributeFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias) {
    ITEntityMain indexTable =
        attributeFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(attributeFilter.getEntity().getName());

    Attribute attribute = attributeFilter.getAttribute();
    SqlField valueField =
        attributeSwapFields.containsKey(attribute)
            ? attributeSwapFields.get(attribute)
            : indexTable.getAttributeValueField(attribute.getName());
    if (attribute.hasRuntimeSqlFunctionWrapper()) {
      valueField = valueField.cloneWithFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper());
    }
    return attributeFilter.hasFunctionTemplate()
        ? apiTranslator.functionFilterSql(
            valueField,
            apiTranslator.functionTemplateSql(attributeFilter.getFunctionTemplate()),
            attributeFilter.getValues(),
            tableAlias,
            sqlParams)
        : apiTranslator.binaryFilterSql(
            valueField,
            attributeFilter.getOperator(),
            attributeFilter.getValues().get(0),
            tableAlias,
            sqlParams);
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.equals(attributeFilter.getAttribute());
  }
}
