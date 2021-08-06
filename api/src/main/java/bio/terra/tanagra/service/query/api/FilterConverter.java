package bio.terra.tanagra.service.query.api;

import bio.terra.common.exception.BadRequestException;
import bio.terra.tanagra.generated.model.ApiArrayFilter;
import bio.terra.tanagra.generated.model.ApiArrayFilterOperator;
import bio.terra.tanagra.generated.model.ApiBinaryFilter;
import bio.terra.tanagra.generated.model.ApiBinaryFilterOperator;
import bio.terra.tanagra.generated.model.ApiFilter;
import bio.terra.tanagra.generated.model.ApiRelationshipFilter;
import bio.terra.tanagra.service.search.Entity;
import bio.terra.tanagra.service.search.EntityVariable;
import bio.terra.tanagra.service.search.Expression.AttributeExpression;
import bio.terra.tanagra.service.search.Expression.Literal;
import bio.terra.tanagra.service.search.Filter;
import bio.terra.tanagra.service.search.Variable;
import bio.terra.tanagra.service.underlay.Underlay;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import java.util.Optional;

/** Converts API filters to Tanagra search {@link bio.terra.tanagra.service.search.Filter}s. */
class FilterConverter {
  private final Underlay underlay;
  private final ExpressionConverter expressionConverter;

  FilterConverter(Underlay underlay) {
    this.underlay = underlay;
    this.expressionConverter = new ExpressionConverter(underlay);
  }

  public Filter convert(ApiFilter apiFilter, VariableScope scope) {
    if (!ConversionUtils.exactlyOneNonNull(
        apiFilter.getArrayFilter(),
        apiFilter.getBinaryFilter(),
        apiFilter.getRelationshipFilter())) {
      throw new BadRequestException(
          "Filter must have exactly one non-null field.\n" + apiFilter.toString());
    }
    if (apiFilter.getArrayFilter() != null) {
      return convert(apiFilter.getArrayFilter(), scope);
    } else if (apiFilter.getBinaryFilter() != null) {
      return convert(apiFilter.getBinaryFilter(), scope);
    } else if (apiFilter.getRelationshipFilter() != null) {
      return convert(apiFilter.getRelationshipFilter(), scope);
    }
    throw new BadRequestException("Unknown Filter type: " + apiFilter.toString());
  }

  @VisibleForTesting
  Filter.ArrayFunction convert(ApiArrayFilter apiArray, VariableScope scope) {
    if (apiArray.getOperands() == null || apiArray.getOperands().isEmpty()) {
      throw new BadRequestException("ArrayFilter must have at least one operand.");
    }
    if (apiArray.getOperands().stream().anyMatch(Objects::isNull)) {
      throw new BadRequestException("ArrayFilter must only have non-null operands.");
    }
    ImmutableList<Filter> filters =
        apiArray.getOperands().stream()
            .map(operand -> convert(operand, scope))
            .collect(ImmutableList.toImmutableList());
    return Filter.ArrayFunction.create(filters, convert(apiArray.getOperator()));
  }

  @VisibleForTesting
  static Filter.ArrayFunction.Operator convert(ApiArrayFilterOperator apiOperator) {
    switch (apiOperator) {
      case AND:
        return Filter.ArrayFunction.Operator.AND;
      case OR:
        return Filter.ArrayFunction.Operator.OR;
      default:
        throw new BadRequestException("Unknown ArrayFilterOperator: " + apiOperator.toString());
    }
  }

  @VisibleForTesting
  Filter.BinaryFunction convert(ApiBinaryFilter apiBinary, VariableScope scope) {
    Preconditions.checkNotNull(
        apiBinary.getAttributeVariable(),
        "BinaryFilter.attributeVariable must not be null.\n%s",
        apiBinary);
    Preconditions.checkNotNull(
        apiBinary.getOperator(), "BinaryFilter.operator must not be null.\n%s", apiBinary);
    Preconditions.checkNotNull(
        apiBinary.getAttributeValue(),
        "BinaryFilter.attributeValue must not be null.\n%s",
        apiBinary);

    AttributeExpression attributeExpression =
        expressionConverter.convert(apiBinary.getAttributeVariable(), scope);
    Filter.BinaryFunction.Operator operator = convert(apiBinary.getOperator());
    Literal literal = expressionConverter.convert(apiBinary.getAttributeValue());
    return Filter.BinaryFunction.create(attributeExpression, operator, literal);
  }

  public Filter.BinaryFunction.Operator convert(ApiBinaryFilterOperator apiOperator) {
    switch (apiOperator) {
      case EQUALS:
        return Filter.BinaryFunction.Operator.EQUALS;
      case LESS_THAN:
        return Filter.BinaryFunction.Operator.LESS_THAN;
      default:
        throw new BadRequestException("Unknown BinaryOperator: " + apiOperator.toString());
    }
  }

  public Filter.RelationshipFilter convert(
      ApiRelationshipFilter apiRelationship, VariableScope scope) {
    Optional<EntityVariable> outerEntityVariable = scope.get(apiRelationship.getOuterVariable());
    if (outerEntityVariable.isEmpty()) {
      throw new BadRequestException(
          String.format(
              "Unknown variable '%s' in RelationshipFilter: %s",
              apiRelationship.getOuterVariable(), apiRelationship));
    }
    Entity newEntity = underlay.entities().get(apiRelationship.getNewEntity());
    if (newEntity == null) {
      throw new BadRequestException(
          String.format(
              "Unable to find entity '%s' in underlay '%s'",
              apiRelationship.getNewEntity(), underlay.name()));
    }
    EntityVariable newEntityVariable =
        EntityVariable.create(newEntity, Variable.create(apiRelationship.getNewVariable()));
    VariableScope innerScope = new VariableScope(scope);
    innerScope.add(newEntityVariable);
    Filter innerFilter = convert(apiRelationship.getFilter(), innerScope);
    return Filter.RelationshipFilter.builder()
        .outerVariable(outerEntityVariable.get())
        .newVariable(newEntityVariable)
        .filter(innerFilter)
        .build();
  }
}
