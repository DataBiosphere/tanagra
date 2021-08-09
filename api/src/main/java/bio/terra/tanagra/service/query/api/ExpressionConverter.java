package bio.terra.tanagra.service.query.api;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.generated.model.ApiAttributeValue;
import bio.terra.tanagra.generated.model.ApiAttributeVariable;
import bio.terra.tanagra.service.search.Attribute;
import bio.terra.tanagra.service.search.AttributeVariable;
import bio.terra.tanagra.service.search.DataType;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression;
import bio.terra.tanagra.service.underlay.Underlay;
import java.util.Optional;

/** Converts API expression to Tanagra search {@link Expression}s. */
class ExpressionConverter {
  private final Underlay underlay;

  ExpressionConverter(Underlay underlay) {
    this.underlay = underlay;
  }

  public Expression.AttributeExpression convert(
      ApiAttributeVariable apiAttributeVariable, VariableScope scope) {
    Optional<EntityVariable> entityVariable = scope.get(apiAttributeVariable.getVariable());
    if (entityVariable.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "Unknown variable '%s' in AttributeVariable: %s",
              apiAttributeVariable.getVariable(), apiAttributeVariable));
    }
    Attribute attribute =
        underlay.attributes().get(entityVariable.get().entity(), apiAttributeVariable.getName());
    if (attribute == null) {
      throw new BadRequestException(
          String.format(
              "Unknown attribute '%s' in entity '%s' where AttributeVariable: %s",
              apiAttributeVariable.getName(),
              entityVariable.get().entity().name(),
              apiAttributeVariable));
    }
    return Expression.AttributeExpression.create(
        AttributeVariable.create(attribute, entityVariable.get().variable()));
  }

  public Expression.Literal convert(ApiAttributeValue apiAttributeValue) {
    if (!ConversionUtils.exactlyOneNonNull(
        apiAttributeValue.isBoolVal(),
        apiAttributeValue.getInt64Val(),
        apiAttributeValue.getStringVal())) {
      throw new BadRequestException(
          "AttributeValue must have exactly one non-null field.\n" + apiAttributeValue.toString());
    }

    if (apiAttributeValue.getStringVal() != null) {
      return Expression.Literal.create(DataType.STRING, apiAttributeValue.getStringVal());
    } else if (apiAttributeValue.getInt64Val() != null) {
      return Expression.Literal.create(DataType.INT64, apiAttributeValue.getInt64Val().toString());
    }
    // TODO implement boolean values.
    throw new BadRequestException("Unknown attribute value type.\n" + apiAttributeValue.toString());
  }
}
