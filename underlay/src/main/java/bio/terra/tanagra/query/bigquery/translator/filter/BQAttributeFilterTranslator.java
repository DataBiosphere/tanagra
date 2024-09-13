package bio.terra.tanagra.query.bigquery.translator.filter;

import bio.terra.tanagra.api.filter.AttributeFilter;
import bio.terra.tanagra.api.shared.*;
import bio.terra.tanagra.exception.*;
import bio.terra.tanagra.query.sql.SqlField;
import bio.terra.tanagra.query.sql.SqlParams;
import bio.terra.tanagra.query.sql.translator.ApiFilterTranslator;
import bio.terra.tanagra.query.sql.translator.ApiTranslator;
import bio.terra.tanagra.underlay.entitymodel.Attribute;
import bio.terra.tanagra.underlay.indextable.ITEntityMain;
import jakarta.annotation.*;

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

    if (attribute.isDataTypeRepeated()) {
      boolean naryOperatorIn =
          (attributeFilter.hasBinaryOperator()
                  && BinaryOperator.EQUALS.equals(attributeFilter.getBinaryOperator()))
              || (attributeFilter.hasNaryOperator()
                  && NaryOperator.IN.equals(attributeFilter.getNaryOperator()));
      boolean naryOperatorNotIn =
          (attributeFilter.hasBinaryOperator()
                  && BinaryOperator.NOT_EQUALS.equals(attributeFilter.getBinaryOperator()))
              || (attributeFilter.hasNaryOperator()
                  && NaryOperator.NOT_IN.equals(attributeFilter.getNaryOperator()));
      if (!naryOperatorIn && !naryOperatorNotIn) {
        throw new InvalidQueryException(
            "Operator not supported for repeated data type attributes: "
                + attributeFilter.getOperatorName()
                + ", "
                + attribute.getName());
      }
      return apiTranslator.naryFilterOnRepeatedFieldSql(
          valueField,
          naryOperatorIn ? NaryOperator.IN : NaryOperator.NOT_IN,
          attributeFilter.getValues(),
          tableAlias,
          sqlParams);
    } else if (attributeFilter.hasUnaryOperator()) {
      return apiTranslator.unaryFilterSql(
          valueField, attributeFilter.getUnaryOperator(), tableAlias, sqlParams);
    } else if (attributeFilter.hasBinaryOperator()) {
      return apiTranslator.binaryFilterSql(
          valueField,
          attributeFilter.getBinaryOperator(),
          attributeFilter.getValues().get(0),
          tableAlias,
          sqlParams);
    } else {
      return apiTranslator.naryFilterSql(
          valueField,
          attributeFilter.getNaryOperator(),
          attributeFilter.getValues(),
          tableAlias,
          sqlParams);
    }
  }

  @Override
  public boolean isFilterOnAttribute(Attribute attribute) {
    return attribute.equals(attributeFilter.getAttribute());
  }
}
