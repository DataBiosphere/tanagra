package bio.terra.tanagra.query2.bigquery.filtertranslator;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.query.FieldPointer;
import bio.terra.tanagra.query2.sql.SqlFilterTranslator;
import bio.terra.tanagra.query2.sql.SqlParams;
import bio.terra.tanagra.query2.sql.SqlTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;

public class BQAttributeFilterTranslator extends SqlFilterTranslator {
  private final AttributeFilter attributeFilter;

  public BQAttributeFilterTranslator(SqlTranslator sqlTranslator, AttributeFilter attributeFilter) {
    super(sqlTranslator);
    this.attributeFilter = attributeFilter;
  }

  @Override
  public String buildSql(SqlParams sqlParams, String tableAlias, FieldPointer idField) {
    ITEntityMain indexTable =
        attributeFilter
            .getUnderlay()
            .getIndexSchema()
            .getEntityMain(attributeFilter.getEntity().getName());

    Attribute attribute = attributeFilter.getAttribute();
    FieldPointer valueField =
        attribute.isId() ? idField : indexTable.getAttributeValueField(attribute.getName());
    if (attribute.hasRuntimeSqlFunctionWrapper()) {
      valueField =
          valueField
              .toBuilder()
              .sqlFunctionWrapper(attribute.getRuntimeSqlFunctionWrapper())
              .build();
    }
    return attributeFilter.hasFunctionTemplate()
        ? sqlTranslator.functionFilterSql(
            valueField,
            sqlTranslator.functionTemplateSql(attributeFilter.getFunctionTemplate()),
            attributeFilter.getValues(),
            tableAlias,
            sqlParams)
        : sqlTranslator.binaryFilterSql(
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
